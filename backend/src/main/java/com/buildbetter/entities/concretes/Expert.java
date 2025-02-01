package com.renovatipoint.entities.concretes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@DiscriminatorValue("EXPERT")
@Table(name = "experts")
@NoArgsConstructor
@AllArgsConstructor
public class Expert extends User {
    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "expert-requests")
    private List<Request> expertRequests = new ArrayList<>();

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "chamber_of_commerce_number")
    private String chamberOfCommerceNumber;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    private String address;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_info_id", referencedColumnName = "id")
    private PaymentInfo paymentInfo;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "job_title_id", referencedColumnName = "id")
    private JobTitle jobTitle;

    @Column(name = "payment_issues_count")
    private int paymentIssuesCount = 0;

    @Column(name = "account_blocked")
    private boolean accountBlocked = false;

    @Column(name = "balance")
    private BigDecimal balance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invoice> expertInvoices = new ArrayList<>();

    public void incrementPaymentIssuesCount() {
        this.paymentIssuesCount++;
        if (this.paymentIssuesCount >= 3) {
            this.accountBlocked = true;
        }
    }

    public void resetPaymentIssuesCount() {
        this.paymentIssuesCount = 0;
        this.accountBlocked = false;
    }
}
