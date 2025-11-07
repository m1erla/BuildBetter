package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.PaymentRetry;
import com.buildbetter.enums.PaymentRetryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRetryRepository extends JpaRepository<PaymentRetry, String> {
    List<PaymentRetry> findBySubscriptionId(String subscriptionId);
    Optional<PaymentRetry> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<PaymentRetry> findByStatus(PaymentRetryStatus status);

    @Query("SELECT pr FROM PaymentRetry pr WHERE pr.nextAttemptAt <= :now AND pr.status = 'RETRYING'")
    List<PaymentRetry> findPendingRetries(LocalDateTime now);

    @Query("SELECT pr FROM PaymentRetry pr WHERE pr.subscription.id = :subscriptionId AND pr.status IN ('PENDING', 'RETRYING')")
    List<PaymentRetry> findActiveRetries(String subscriptionId);
}
