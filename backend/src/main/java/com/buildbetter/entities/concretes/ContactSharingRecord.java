package com.renovatipoint.entities.concretes;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_sharing_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactSharingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "expert_id", nullable = false)
    private Expert expert;

    @ManyToOne
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn(name = "ad_id", nullable = false)
    private Ads ad;

    @Column(name = "shared_at", nullable = false)
    private LocalDateTime sharedAt;

    @Column(name = "payment_processed")
    private Boolean paymentProcessed;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @PrePersist
    protected void onCreate() {
        sharedAt = LocalDateTime.now();
    }
}