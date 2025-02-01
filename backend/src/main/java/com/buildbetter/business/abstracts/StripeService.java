package com.renovatipoint.business.abstracts;

import com.renovatipoint.business.requests.SepaPaymentRequest;
import com.renovatipoint.entities.concretes.Expert;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;

import java.math.BigDecimal;

public interface StripeService {
        PaymentIntent createSepaPaymentIntent(String customerId, String paymentMethodId, double amount)
                        throws StripeException;

        SetupIntent createSepaSetupIntent(String customerId) throws StripeException;

        String retrievePaymentReceiptUrl(String paymentIntentId) throws StripeException;

        String createStripeCustomer(String email, String name) throws StripeException;

        void deleteStripeCustomer(String customerId);

        void attachPaymentMethodToCustomer(String customerId, String paymentMethodId) throws StripeException;

        PaymentIntent processSepaPayment(String customerId, String paymentMethodId, BigDecimal amount)
                        throws StripeException;

        void savePaymentInfo(SepaPaymentRequest request, Expert expert);

        String createAndConfirmPayment(String customerId, BigDecimal amount, String currency, String description)
                        throws StripeException;

        PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException;
}