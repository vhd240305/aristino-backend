package com.aristino.backend.service;

import com.aristino.backend.entity.Faq;
import com.aristino.backend.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Tra loi theo luat, khong can AI: chao hoi -> cau hoi thuong gap (bang faqs) -> tim san pham theo tu khoa.
 * Dung khi chua co Gemini API key, khi Gemini het luot / bi loi, hoac khi qua toc do goi AI.
 */
@Component
@RequiredArgsConstructor
public class RuleBasedResponder {

    private static final Set<String> GREETINGS = Set.of("chao", "hello", "hi", "alo", "hey", "helo");

    private final FaqRepository faqRepository;
    private final ChatCatalogService catalog;

    public String reply(String message) {
        List<String> words = ChatCatalogService.rawWords(message);

        // 1. Chao hoi (cau ngan co tu chao)
        if (words.size() <= 5 && words.stream().anyMatch(GREETINGS::contains)) {
            return ChatTexts.GREETING;
        }

        // 2. Cau hoi thuong gap: chon FAQ co nhieu tu khoa trung nhat (it nhat 2 tu)
        Set<String> wanted = ChatCatalogService.tokensOf(message);
        Faq bestFaq = null;
        int bestScore = 1;
        for (Faq faq : faqRepository.findAll()) {
            int score = 0;
            for (String token : ChatCatalogService.tokensOf(faq.getQuestion())) {
                if (wanted.contains(token)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestFaq = faq;
            }
        }
        if (bestFaq != null && bestFaq.getAnswer() != null && !bestFaq.getAnswer().isBlank()) {
            return bestFaq.getAnswer();
        }

        // 3. Tim san pham theo tu khoa
        List<ChatCatalogService.Item> found = catalog.relevant(message, List.of(), 3);
        if (!found.isEmpty()) {
            StringBuilder sb = new StringBuilder(ChatTexts.PRODUCT_INTRO);
            for (ChatCatalogService.Item item : found) {
                sb.append("\n- ").append(item.name()).append(" (").append(ChatCatalogService.money(item.minPrice()))
                        .append("): /products/").append(item.slug());
            }
            return sb.toString();
        }

        return ChatTexts.NOT_FOUND;
    }
}