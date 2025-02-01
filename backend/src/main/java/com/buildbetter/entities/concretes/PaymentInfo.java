package com.renovatipoint.entities.concretes;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne(mappedBy = "paymentInfo")
    private Expert expert;
    @Column(name = "iban", nullable = false)
    private String iban;
    @Column(name = "bic", nullable = false)
    private String bic;
    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "stripe_customer_id", nullable = false)
    private String stripeCustomerId;

    @Column(name = "payment_method_id", nullable = false)
    private String paymentMethodId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_payment_status")
    private String lastPaymentStatus;

    @Column(name = "last_payment_intent_id")
    private String lastPaymentIntentId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (isActive == null) {
            isActive = true;
        }
    }
}
