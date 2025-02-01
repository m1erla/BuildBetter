package com.renovatipoint.business.abstracts;

import com.renovatipoint.entities.concretes.Invoice;
import com.renovatipoint.enums.PaymentType;
import com.stripe.exception.StripeException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface InvoiceService {

  List<Invoice> findByUserId(String userId);

  List<Invoice> findByExpertId(String expertId);

  Optional<Invoice> findById(String invoiceId);

  Invoice findByPaymentMethodId(String paymentMethodId);

  Invoice findByStripeCustomerId(String customerId);

  void save(Invoice invoice);

  Invoice generateInvoiceForUser(String userId, double amount, String paymentIntentId)
      throws IOException, StripeException;

  Invoice generateInvoiceForExpert(String expertId, double amount, String paymentIntentId, PaymentType paymentType)
      throws IOException, StripeException;

  byte[] getInvoicePDF(String invoiceId) throws IOException;

}
