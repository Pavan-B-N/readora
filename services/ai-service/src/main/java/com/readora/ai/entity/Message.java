package com.readora.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** One turn in a conversation — this is conversationMemory's backing store. */
@Entity
@Table(name = "messages", schema = "ai")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Books the reply recommended — only ever populated on ASSISTANT messages, empty for USER ones. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "book_ids", columnDefinition = "text[]")
    private List<String> bookIds = List.of();

    protected Message() {
    }

    public Message(Conversation conversation, MessageRole role, String content) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
    }

    public Message(Conversation conversation, MessageRole role, String content, List<String> bookIds) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.bookIds = bookIds;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Never null even if the column is — rows written before this field existed have NULL there. */
    public List<String> getBookIds() {
        return bookIds != null ? bookIds : List.of();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Message message)) return false;
        return id != null && Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
