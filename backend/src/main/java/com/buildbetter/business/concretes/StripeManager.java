package com.renovatipoint.business.concretes;

import com.renovatipoint.business.abstracts.InvoiceService;
import com.renovatipoint.business.abstracts.StripeService;
import com.renovatipoint.business.abstracts.UserService;
import com.renovatipoint.business.requests.SepaPaymentRequest;
import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.dataAccess.abstracts.PaymentInfoRepository;
import com.renovatipoint.entities.concretes.Expert;
import com.renovatipoint.entities.concretes.Invoice;
import com.renovatipoint.entities.concretes.PaymentInfo;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class StripeManager implements StripeService {

        private final InvoiceManager invoiceManager;
        private final ChatManager chatManager;
        private final UserService userService;
        private final InvoiceService invoiceService;
        private final PaymentInfoRepository paymentInfoRepository;

        @Value("${stripe.api-key}")
        private final String secretApiKey;

        @Autowired
        public StripeManager(@Lazy InvoiceManager invoiceManager, @Lazy ChatManager chatManager,
                        @Value("${stripe.api-key}") String secretApiKey,
                        @Lazy InvoiceService invoiceService,
                        @Lazy UserService userService, @Lazy PaymentInfoRepository paymentInfoRepository) {
                this.invoiceManager = invoiceManager;
                this.chatManager = chatManager;
                this.secretApiKey = secretApiKey;
                this.invoiceService = invoiceService;
                this.userService = userService;
                this.paymentInfoRepository = paymentInfoRepository;
                Stripe.apiKey = secretApiKey;
        }

        @PostConstruct
        public void init() {
                Stripe.apiKey = secretApiKey;
        }

        @Override
        public PaymentIntent createSepaPaymentIntent(String customerId, String paymentMethodId, double amount)
                        throws StripeException {
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                                .setAmount((long) (amount * 100)) // Convert to cents
                                .setCurrency("eur")
                                .setCustomer(customerId)
                                .setPaymentMethod(paymentMethodId) // Attach the SEPA payment method
                                .setConfirm(true) // Confirm the payment after the method is attached
                                .setPaymentMethodOptions(
                                                PaymentIntentCreateParams.PaymentMethodOptions.builder()
                                                                .setSepaDebit(PaymentIntentCreateParams.PaymentMethodOptions.SepaDebit
                                                                                .builder()
                                                                                .setMandateOptions(
                                                                                                PaymentIntentCreateParams.PaymentMethodOptions.SepaDebit.MandateOptions
                                                                                                                .builder()
                                                                                                                .build())
                                                                                .build())
                                                                .build())
                                .setMandateData(PaymentIntentCreateParams.MandateData.builder()
                                                .setCustomerAcceptance(
                                                                PaymentIntentCreateParams.MandateData.CustomerAcceptance
                                                                                .builder()
                                                                                .setType(PaymentIntentCreateParams.MandateData.CustomerAcceptance.Type.OFFLINE)
                                                                                .build())
                                                .build())
                                .setReturnUrl("http://localhost:8080/payment-confirmation")
                                .setConfirm(true)
                                .build();

                return PaymentIntent.create(params);
        }

        @Override
        public SetupIntent createSepaSetupIntent(String customerId) throws StripeException {
                SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                                .setCustomer(customerId)
                                .addPaymentMethodType("sepa_debit")
                                .build();
                return SetupIntent.create(params);
        }

        @Override
        public String retrievePaymentReceiptUrl(String paymentIntentId) throws StripeException {
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                ChargeListParams chargeListParams = ChargeListParams.builder()
                                .setPaymentIntent(paymentIntentId)
                                .build();

                ChargeCollection charges = Charge.list(chargeListParams);
                if (!charges.getData().isEmpty()) {
                        Charge charge = charges.getData().get(0);
                        return charge.getReceiptUrl();
                } else {
                        throw new IllegalStateException("No charges found for PaymentIntent: " + paymentIntentId);
                }
        }

        @Override
        public String createStripeCustomer(String email, String name) throws StripeException {
                CustomerCreateParams params = CustomerCreateParams.builder()
                                .setEmail(email)
                                .setName(name)
                                .build();
                Customer customer = Customer.create(params);
                return customer.getId();
        }

        @Override
        public void deleteStripeCustomer(String customerId) {
                try {
                        Customer customer = Customer.retrieve(customerId);
                        customer.delete();
                        log.info("Stripe customer deleted successfully: {}", customerId);
                } catch (StripeException e) {
                        log.error("Error deleting Stripe customer: {}", customerId, e);
                        throw new BusinessException("Failed to delete Stripe customer: " + e.getMessage());
                }
        }

        @Override
        public void attachPaymentMethodToCustomer(String customerId, String paymentMethodId) throws StripeException {
                PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
                paymentMethod.attach(PaymentMethodAttachParams.builder()
                                .setCustomer(customerId)
                                .build());
        }

        @Override
        public PaymentIntent processSepaPayment(String customerId, String paymentMethodId, BigDecimal amount)
                        throws StripeException {
                try {
                        log.info("Processing SEPA payment for customer: {}, amount: {}", customerId, amount);

                        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                                        .setAmount(amount.multiply(new BigDecimal("100")).longValue()) // Convert to
                                                                                                       // cents
                                        .setCurrency("eur")
                                        .setCustomer(customerId)
                                        .setPaymentMethod(paymentMethodId)
                                        .setConfirm(true)
                                        .setOffSession(true)
                                        .addPaymentMethodType("sepa_debit")
                                        .setPaymentMethodOptions(
                                                        PaymentIntentCreateParams.PaymentMethodOptions.builder()
                                                                        .setSepaDebit(
                                                                                        PaymentIntentCreateParams.PaymentMethodOptions.SepaDebit
                                                                                                        .builder()
                                                                                                        .setMandateOptions(
                                                                                                                        PaymentIntentCreateParams.PaymentMethodOptions.SepaDebit.MandateOptions
                                                                                                                                        .builder()
                                                                                                                                        .build())
                                                                                                        .build())
                                                                        .build())
                                        .setMandateData(
                                                        PaymentIntentCreateParams.MandateData.builder()
                                                                        .setCustomerAcceptance(
                                                                                        PaymentIntentCreateParams.MandateData.CustomerAcceptance
                                                                                                        .builder()
                                                                                                        .setType(PaymentIntentCreateParams.MandateData.CustomerAcceptance.Type.OFFLINE)
                                                                                                        .build())
                                                                        .build())
                                        .build();

                        PaymentIntent paymentIntent = PaymentIntent.create(params);
                        log.info("Payment intent created with status: {}", paymentIntent.getStatus());

                        return paymentIntent;
                } catch (StripeException e) {
                        log.error("Stripe payment processing failed", e);
                        throw e;
                }
        }

        @Override
        public void savePaymentInfo(SepaPaymentRequest request, Expert expert) {
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setIban(request.getIban());
                paymentInfo.setBic(request.getBic());
                paymentInfo.setBankName(request.getBankName());
                paymentInfo.setStripeCustomerId(expert.getStripeCustomerId());
                paymentInfo.setPaymentMethodId(request.getPaymentMethodId());

                expert.setPaymentInfo(paymentInfo);
                paymentInfoRepository.save(paymentInfo);
        }

        @Override
        public String createAndConfirmPayment(String customerId, BigDecimal amount, String currency, String description)
                        throws StripeException {
                try {
                        log.info("Creating and confirming payment for customer: {}, amount: {}, currency: {}",
                                        customerId, amount, currency);

                        // Önce müşterinin default payment method'unu alalım
                        PaymentMethodListParams listParams = PaymentMethodListParams.builder()
                                        .setCustomer(customerId)
                                        .setType(PaymentMethodListParams.Type.SEPA_DEBIT)
                                        .build();

                        PaymentMethodCollection paymentMethods = PaymentMethod.list(listParams);
                        if (paymentMethods.getData().isEmpty()) {
                                throw new BusinessException("No SEPA payment method found for customer");
                        }

                        String paymentMethodId = paymentMethods.getData().get(0).getId();

                        // PaymentIntent'i oluşturalım - Düzeltilmiş amount dönüşümü
                        PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                                        .setAmount(amount.longValue() * 100) // Düzeltildi: Euro'yu cent'e çevirme
                                        .setCurrency(currency.toLowerCase())
                                        .setCustomer(customerId)
                                        .setPaymentMethod(paymentMethodId)
                                        .setDescription(description)
                                        .setConfirm(true)
                                        .setOffSession(true)
                                        .addPaymentMethodType("sepa_debit")
                                        .setPaymentMethodOptions(
                                                        PaymentIntentCreateParams.PaymentMethodOptions.builder()
                                                                        .setSepaDebit(
                                                                                        PaymentIntentCreateParams.PaymentMethodOptions.SepaDebit
                                                                                                        .builder()
                                                                                                        .setMandateOptions(
                                                                                                                        PaymentIntentCreateParams.PaymentMethodOptions.SepaDebit.MandateOptions
                                                                                                                                        .builder()
                                                                                                                                        .build())
                                                                                                        .build())
                                                                        .build())
                                        .setMandateData(
                                                        PaymentIntentCreateParams.MandateData.builder()
                                                                        .setCustomerAcceptance(
                                                                                        PaymentIntentCreateParams.MandateData.CustomerAcceptance
                                                                                                        .builder()
                                                                                                        .setType(PaymentIntentCreateParams.MandateData.CustomerAcceptance.Type.OFFLINE)
                                                                                                        .build())
                                                                        .build())
                                        .build();

                        PaymentIntent paymentIntent = PaymentIntent.create(createParams);
                        log.info("Payment intent created with status: {}", paymentIntent.getStatus());

                        // PaymentIntent'i onaylayalım
                        if ("requires_confirmation".equals(paymentIntent.getStatus())) {
                                PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                                                .setPaymentMethod(paymentMethodId)
                                                .build();
                                paymentIntent = paymentIntent.confirm(confirmParams);
                                log.info("Payment intent confirmed with status: {}", paymentIntent.getStatus());
                        }

                        return paymentIntent.getId();
                } catch (StripeException e) {
                        log.error("Stripe payment processing failed", e);
                        throw e;
                }
        }

        public void handleSuccessfulPayment(Event event) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject()
                                .orElseThrow();

                String paymentMethodId = paymentIntent.getId();
                String customerId = paymentIntent.getCustomer();
                long amountReceived = paymentIntent.getAmountReceived();

                Invoice invoice = invoiceService.findByPaymentMethodId(paymentMethodId);
                if (invoice == null) {
                        invoice = invoiceService.findByStripeCustomerId(customerId);
                }

                if (invoice != null) {
                        invoice.setPaid(true);
                        invoice.setPaymentMethodId(paymentMethodId);
                        invoice.setAmount(BigDecimal.valueOf(amountReceived / 100.0));
                        invoice.setDateIssued(LocalDateTime.now());
                        invoiceService.save(invoice);

                        userService.sendPaymentConfirmationEmail(invoice.getUser().getEmail(), invoice);

                        System.out.println("Payment succeeded for PaymentIntent: " + paymentMethodId);
                }
        }

        public void handleFailedPayment(Event event) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject()
                                .orElseThrow();

                String paymentIntentId = paymentIntent.getId();
                String customerId = paymentIntent.getCustomer();

                Invoice invoice = invoiceService.findByPaymentMethodId(paymentIntentId);
                if (invoice == null) {
                        invoice = invoiceService.findByStripeCustomerId(customerId);
                }

                if (invoice != null) {
                        invoice.setPaid(false);
                        invoiceService.save(invoice);

                        userService.sendPaymentFailureEmail(invoice.getUser().getEmail(), invoice);

                        System.out.println("Payment failed for PaymentIntent: " + paymentIntentId);
                }
        }

        @Override
        public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
                try {
                        log.info("Retrieving payment intent with ID: {}", paymentIntentId);
                        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                        log.info("Payment intent retrieved successfully. Status: {}", paymentIntent.getStatus());
                        return paymentIntent;
                } catch (StripeException e) {
                        log.error("Error retrieving payment intent: {}", paymentIntentId, e);
                        throw e;
                }
        }
}
