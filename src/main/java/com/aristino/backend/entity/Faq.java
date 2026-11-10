package com.aristino.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    // Dung khi lam chatbot theo huong RAG, co the bo qua neu khong can
    @Column(name = "embedding_json", columnDefinition = "JSON")
    private String embeddingJson;
}
