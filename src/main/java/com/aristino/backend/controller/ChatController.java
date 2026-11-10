package com.aristino.backend.controller;

import com.aristino.backend.dto.chat.ChatMessageDto;
import com.aristino.backend.dto.chat.ChatRequest;
import com.aristino.backend.dto.chat.ChatResponse;
import com.aristino.backend.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/chat/** da duoc SecurityConfig cho phep ca khach chua dang nhap
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // POST /api/chat/messages - gui 1 tin nhan, nhan cau tra loi
    @PostMapping("/messages")
    public ChatResponse send(@Valid @RequestBody ChatRequest request, HttpServletRequest http) {
        return chatService.chat(request.getSessionToken(), request.getMessage(), http.getRemoteAddr());
    }

    // GET /api/chat/history?sessionToken=... - lich su cua phien chat
    @GetMapping("/history")
    public List<ChatMessageDto> history(@RequestParam(required = false) String sessionToken) {
        return chatService.history(sessionToken);
    }
}