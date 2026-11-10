package com.aristino.backend.service;

import com.aristino.backend.dto.chat.ChatMessageDto;
import com.aristino.backend.dto.chat.ChatResponse;
import com.aristino.backend.entity.ChatMessage;
import com.aristino.backend.entity.ChatSession;
import com.aristino.backend.entity.Faq;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.ChatMessageRepository;
import com.aristino.backend.repository.ChatSessionRepository;
import com.aristino.backend.repository.FaqRepository;
import com.aristino.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Chatbot tu van khach hang. Moi tin nhan:
 *   luu tin nhan khach -> (neu co Gemini) ghep loi nhac gom luat + du lieu san pham/FAQ that + lich su -> goi Gemini
 *   -> neu khong duoc thi tra loi theo luat -> luu cau tra loi -> tra ve.
 * Khong gui thong tin ca nhan (ten, so dien thoai, dia chi, don hang) cho Gemini.
 * Co chu y KHONG danh dau @Transactional ca ham: goi mang mat vai giay, khong giu ket noi database trong luc cho.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int HISTORY_FOR_AI = 8;
    private static final int HISTORY_FOR_UI = 40;
    private static final int MAX_FAQ_FOR_AI = 30;
    private static final int MAX_REPLY_LENGTH = 2000;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final FaqRepository faqRepository;
    private final UserRepository userRepository;
    private final ChatCatalogService catalog;
    private final RuleBasedResponder ruleBasedResponder;
    private final GeminiClient geminiClient;
    private final ChatRateLimiter rateLimiter;

    public ChatResponse chat(String sessionToken, String rawMessage, String clientKey) {
        String message = rawMessage.trim();

        if (!rateLimiter.allowUser(clientKey)) {
            return new ChatResponse(sessionToken, ChatTexts.TOO_FAST, "LIMITED");
        }

        ChatSession session = findOrCreateSession(sessionToken);

        // Lay lich su TRUOC khi luu tin nhan moi de khong bi lap
        List<ChatMessage> history = recentMessages(session.getId(), HISTORY_FOR_AI);
        save(session, ChatMessage.Sender.USER, message);

        String reply = null;
        String mode = "RULES";
        if (geminiClient.isAvailable() && rateLimiter.allowAiCall()) {
            reply = geminiClient.generate(buildSystemInstruction(message, history), buildInput(history, message)).orElse(null);
            if (reply != null) {
                mode = "AI";
            }
        }
        if (reply == null) {
            reply = ruleBasedResponder.reply(message);
        }
        if (reply.length() > MAX_REPLY_LENGTH) {
            reply = reply.substring(0, MAX_REPLY_LENGTH);
        }

        save(session, ChatMessage.Sender.BOT, reply);
        return new ChatResponse(session.getSessionToken(), reply, mode);
    }

    // Lich su de frontend dung lai khung chat sau khi tai lai trang
    public List<ChatMessageDto> history(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return List.of();
        }
        return sessionRepository.findBySessionToken(sessionToken)
                .map(session -> recentMessages(session.getId(), HISTORY_FOR_UI).stream().map(ChatMessageDto::from).toList())
                .orElse(List.of());
    }

    // ===== Phien chat =====

    private ChatSession findOrCreateSession(String sessionToken) {
        User user = currentUser();
        if (sessionToken != null && !sessionToken.isBlank()) {
            ChatSession existing = sessionRepository.findBySessionToken(sessionToken).orElse(null);
            if (existing != null) {
                // Khach dang nhap giua chung: gan phien nay vao tai khoan
                if (existing.getUser() == null && user != null) {
                    existing.setUser(user);
                    sessionRepository.save(existing);
                }
                return existing;
            }
        }
        // Ma phien luon do server tao (khong nhan ma do client tu dat)
        return sessionRepository.save(ChatSession.builder()
                .sessionToken(UUID.randomUUID().toString())
                .user(user)
                .build());
    }

    // Nguoi dang nhap (neu co). Chatbot cho phep khach vang lai nen co the tra ve null
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private void save(ChatSession session, ChatMessage.Sender sender, String content) {
        messageRepository.save(ChatMessage.builder().session(session).sender(sender).content(content).build());
    }

    // N tin nhan gan nhat theo thu tu thoi gian tang dan
    private List<ChatMessage> recentMessages(Long sessionId, int limit) {
        List<ChatMessage> latest = new ArrayList<>(
                messageRepository.findBySessionId(sessionId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"))));
        Collections.reverse(latest);
        return latest;
    }

    // ===== Ghep loi nhac cho Gemini =====

    private String buildSystemInstruction(String message, List<ChatMessage> history) {
        List<String> recentUserTexts = history.stream()
                .filter(m -> m.getSender() == ChatMessage.Sender.USER)
                .map(ChatMessage::getContent)
                .toList();

        StringBuilder sb = new StringBuilder(ChatTexts.SYSTEM_PROMPT);
        sb.append("\n\n").append(catalog.contextFor(message, recentUserTexts));

        List<Faq> faqs = faqRepository.findAll();
        if (!faqs.isEmpty()) {
            sb.append('\n').append(ChatTexts.FAQ_HEADER).append('\n');
            faqs.stream().limit(MAX_FAQ_FOR_AI).forEach(faq ->
                    sb.append("H: ").append(faq.getQuestion()).append("\n\u0110: ").append(faq.getAnswer()).append('\n'));
        }
        return sb.toString();
    }

    private String buildInput(List<ChatMessage> history, String message) {
        StringBuilder sb = new StringBuilder();
        if (!history.isEmpty()) {
            sb.append(ChatTexts.HISTORY_HEADER).append('\n');
            for (ChatMessage m : history) {
                sb.append(m.getSender() == ChatMessage.Sender.USER ? ChatTexts.CUSTOMER : ChatTexts.ASSISTANT)
                        .append(m.getContent()).append('\n');
            }
            sb.append('\n');
        }
        sb.append(ChatTexts.NEW_QUESTION).append(message).append("\n\n").append(ChatTexts.ANSWER_NOW);
        return sb.toString();
    }
}