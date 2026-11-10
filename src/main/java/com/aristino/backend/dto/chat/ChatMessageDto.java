package com.aristino.backend.dto.chat;

import com.aristino.backend.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageDto(String sender, String content, LocalDateTime createdAt) {

    public static ChatMessageDto from(ChatMessage m) {
        return new ChatMessageDto(m.getSender().name(), m.getContent(), m.getCreatedAt());
    }
}