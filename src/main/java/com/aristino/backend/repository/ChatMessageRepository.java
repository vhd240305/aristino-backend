package com.aristino.backend.repository;

import com.aristino.backend.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    // Lay N tin nhan gan nhat cua mot phien (truyen Pageable co sap xep giam dan theo id)
    List<ChatMessage> findBySessionId(Long sessionId, Pageable pageable);
}