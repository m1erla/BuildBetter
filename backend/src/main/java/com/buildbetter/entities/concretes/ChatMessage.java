package com.renovatipoint.entities.concretes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.renovatipoint.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.lang.annotation.Documented;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    private LocalDateTime timestamp;

    @Column(name = "is_read")
    private boolean isRead;

    // Renamed for clarity and consistency
    @Column(name = "is_contact_info")
    private Boolean contactInfo = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType;

    // Add payment reference for contact sharing and job completion
    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    // Add amount for payment tracking
    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Column(name = "payment_currency")
    private String paymentCurrency;

    // Add a safe setter
    public void setContactInfo(Boolean contactInfo) {
        this.contactInfo = contactInfo != null ? contactInfo : false;
    }
    public boolean isContactInfo() {
        return Boolean.TRUE.equals(this.contactInfo);
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        isRead = false;

        // Set appropriate message type based on content type
        if (messageType == null) {
            if (contactInfo) {
                messageType = MessageType.CONTACT_INFO;
            } else {
                messageType = MessageType.CHAT;
            }
        }

        // Set default currency for payments if applicable
        if (paymentAmount != null && paymentCurrency == null) {
            paymentCurrency = "EUR";
        }
        // Ensure isContactInfo is never null
        if (contactInfo == null) {
            contactInfo = false;
        }
    }

    // Helper method to determine if this message represents a completed payment
    public boolean isPaymentComplete() {
        return paymentIntentId != null &&
                (messageType == MessageType.CONTACT_INFO ||
                        messageType == MessageType.JOB_COMPLETION);
    }

    // Helper method to check if this is a system notification
    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM ||
                messageType == MessageType.NOTIFICATION;
    }

    // Helper method for contact information messages
    public boolean isContactInformation() {
        return messageType == MessageType.CONTACT_INFO || contactInfo;
    }

    // Helper method for job completion messages
    public boolean isJobCompletion() {
        return messageType == MessageType.JOB_COMPLETION;
    }
}
