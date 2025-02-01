package com.renovatipoint.entities.concretes;

import com.renovatipoint.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    private String title;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String relatedEntityId;

    private LocalDateTime createdAt;
    @Column(name = "is_read")
    private boolean isRead;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        isRead = false;
    }
}
