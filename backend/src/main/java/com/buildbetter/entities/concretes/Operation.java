//package com.renovatipoint.entities.concretes;
//
//import com.renovatipoint.enums.OperationStatus;
//import com.renovatipoint.enums.OperationType;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Date;
//
//@Entity
//@Table(name = "transactions")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Data
//@Builder
//public class Operation {
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private String id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id")
//    private User user;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "expert_id")
//    private Expert expert;
//
//    @OneToOne
//    @JoinColumn(name = "invoice_id")
//    private Invoice invoice;
//
//    @Column(nullable = false)
//    private BigDecimal amount;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private OperationType type;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private OperationStatus status;
//
//    @Column(name = "stripe_payment_intent_id")
//    private String paymentMethodId;
//
//    @Column(name = "created_at", nullable = false)
//    private Date createdAt;
//
//
//}
