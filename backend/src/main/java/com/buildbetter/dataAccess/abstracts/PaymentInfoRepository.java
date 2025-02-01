package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.PaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, String> {

    Optional<PaymentInfo> findByStripeCustomerId(String stripeCustomerId);

    Optional<PaymentInfo> findByIban(String iban);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentInfo p SET p.isActive = false WHERE p.id = :id")
    void deactivatePaymentInfo(String id);
}
