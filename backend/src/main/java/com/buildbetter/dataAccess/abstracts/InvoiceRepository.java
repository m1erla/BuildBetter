package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    List<Invoice> findByUserId(String userId);

    List<Invoice> findByExpertId(String expertId);

    @Query("SELECT i FROM Invoice i WHERE i.expert.paymentInfo.paymentMethodId = :paymentMethodId")
    Invoice findByPaymentMethodId(@Param("paymentMethodId") String paymentMethodId);


    @Query("SELECT i FROM Invoice i WHERE i.expert.paymentInfo.stripeCustomerId = :stripeCustomerId")
    Invoice findByStripeCustomerId(@Param("stripeCustomerId") String stripeCustomerId);


}
