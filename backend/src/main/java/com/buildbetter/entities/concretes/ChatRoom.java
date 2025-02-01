package com.renovatipoint.entities.concretes;

import com.renovatipoint.enums.ChatRoomStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String chatId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "expert_id")
    private Expert expert;

    @ManyToOne
    @JoinColumn(name = "ad_id")
    private Ads ad;

    @OneToOne
    @JoinColumn(name = "request_id")
    private Request request;

    private Boolean active;

    @Column(name = "expert_blocked")
    private Boolean expertBlocked = false;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    @OrderBy("timestamp DESC")
    private List<ChatMessage> messages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;
    @Column(name = "is_completed")
    private Boolean completed = false;

    @Column(name = "completion_payment_processed")
    private Boolean completionPaymentProcessed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "contact_shared")
    private Boolean contactShared = false;

    @Column(name = "contact_shared_at")
    private LocalDateTime contactSharedAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<ContactSharingRecord> contactSharingRecords = new ArrayList<>();

    // Helper methods for contact sharing
    public boolean isContactShared() {
        return Boolean.TRUE.equals(contactShared);
    }

    public void setContactShared(boolean shared) {
        this.contactShared = shared;
        if (shared) {
            this.contactSharedAt = LocalDateTime.now();
        }
    }

    // Helper methods for chat room status
    public boolean isCompleted() {
        return Boolean.TRUE.equals(completed);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public boolean isExpertBlocked() {
        return Boolean.TRUE.equals(expertBlocked);
    }

    public boolean isCompletionPaymentProcessed() {
        return Boolean.TRUE.equals(completionPaymentProcessed);
    }

    // Method to check if contact has been shared between specific user and expert
    public boolean hasContactBeenShared(String userId, String expertId) {
        return contactSharingRecords.stream()
                .anyMatch(record ->
                        record.getUser().getId().equals(userId) &&
                                record.getExpert().getId().equals(expertId));
    }

    // Method to handle completion
    public void markAsCompleted(String paymentIntentId) {
        this.completed = true;
        this.completionPaymentProcessed = true;
        this.completedAt = LocalDateTime.now();
        this.status = ChatRoomStatus.COMPLETED;
        this.active = false;
        this.stripePaymentIntentId = paymentIntentId;
    }

    @PrePersist
    protected void onCreate() {
        lastActivity = LocalDateTime.now();
        active = true;
        status = ChatRoomStatus.ACTIVE;
        expertBlocked = expert != null && expert.isAccountBlocked();
        completed = false;
        completionPaymentProcessed = false;
        contactShared = false;
    }

    // Method to update activity
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    // Method to check if messages can be sent
    public boolean canSendMessages() {
        return isActive() && !isCompleted() && status == ChatRoomStatus.ACTIVE;
    }

    // Method to check if contact can be shared
    public boolean canShareContact() {
        return isActive() && !isContactShared() && !isCompleted();
    }

}
