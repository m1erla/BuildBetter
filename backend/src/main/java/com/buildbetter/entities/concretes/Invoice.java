package com.renovatipoint.entities.concretes;

import com.renovatipoint.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "expert_id")
    private Expert expert;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "date_issued", nullable = false)
    private LocalDateTime dateIssued;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "stripe_receipt_url")
    private String stripeReceiptUrl;

    @Column(name = "stripe_payment_intent_id")
    private String paymentMethodId;

    @Column(name = "stripe_invoice_id")
    private String stripeInvoiceId;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    // @OneToOne(mappedBy = "invoice", cascade = CascadeType.ALL)
    // private Operation operation;

}
