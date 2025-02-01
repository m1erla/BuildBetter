package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.concretes.StripeManager;
import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.dataAccess.abstracts.ExpertRepository;
import com.renovatipoint.entities.concretes.Expert;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class StripePaymentController {

    private final StripeManager stripeManager;
    private final ExpertRepository expertRepository;

    private final String stripeWebhookSecret;

    @Autowired
    public StripePaymentController(
            StripeManager stripeManager,
            ExpertRepository expertRepository,
            @Value("${stripe.webhook-secret}") String stripeWebhookSecret) {
        this.stripeManager = stripeManager;
        this.expertRepository = expertRepository;
        this.stripeWebhookSecret = stripeWebhookSecret;
    }

    // Create SEPA Payment Intent
    @PostMapping("/create-sepa-payment-intent")
    public ResponseEntity<?> createSepaPaymentIntent(@RequestBody Map<String, Object> paymentData) {
        try {
            String customerId = (String) paymentData.get("customerId");
            String paymentMethodId = (String) paymentData.get("paymentMethodId");
            double amount = Double.parseDouble(paymentData.get("amount").toString());

            // Create PaymentIntent via StripeManager
            PaymentIntent paymentIntent = stripeManager.createSepaPaymentIntent(customerId, paymentMethodId, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("id", paymentIntent.getId());
            response.put("status", paymentIntent.getStatus());
            response.put("amount", paymentIntent.getAmount());
            response.put("currency", paymentIntent.getCurrency());
            response.put("client_secret", paymentIntent.getClientSecret());

            return ResponseEntity.ok(response);
        } catch (StripeException ex) {
            // Return a specific error message with error details
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create SEPA payment intent: " + ex.getMessage());
        } catch (Exception e) {
            // Handle generic errors
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid input data: " + e.getMessage());
        }
    }

    // Handle Stripe Webhook events
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().collect(Collectors.joining());
        String sigHeader = request.getHeader("Stripe-Signature");

        try {
            // Construct and verify the event using the Stripe webhook secret
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);

            // Handle different event types
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    stripeManager.handleSuccessfulPayment(event);
                    break;
                case "payment_intent.payment_failed":
                    stripeManager.handleFailedPayment(event);
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unhandled event type");
            }

            return ResponseEntity.ok("Webhook received and processed.");
        } catch (SignatureVerificationException e) {
            // Return error if the signature is invalid
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error: " + e.getMessage());
        }
    }

    // Charge customer using SEPA direct debit
    @PostMapping("/charge")
    public ResponseEntity<?> chargeCustomer(@RequestBody Map<String, Object> requestBody) {
        try {
            String expertId = (String) requestBody.get("expertId");
            String stripeCustomerId = (String) requestBody.get("stripeCustomerId");
            String paymentMethodId = (String) requestBody.get("paymentMethodId");
            BigDecimal amount = new BigDecimal(requestBody.get("amount").toString());
            String reason = (String) requestBody.get("reason");

            log.info(
                    "Processing payment. ExpertId: {}, StripeCustomerId: {}, PaymentMethodId: {}, Amount: {}, Reason: {}",
                    expertId, stripeCustomerId, paymentMethodId, amount, reason);

            // Verify the expert exists and has payment method
            Expert expert = expertRepository.findById(expertId)
                    .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

            if (expert.getPaymentInfo() == null) {
                return ResponseEntity.badRequest()
                        .body(new BusinessException("Expert has not set up payment information"));
            }

            // Stripe customer ID kontrolü
            String finalStripeCustomerId = stripeCustomerId != null ? stripeCustomerId : expert.getStripeCustomerId();
            if (finalStripeCustomerId == null || !finalStripeCustomerId.startsWith("cus_")) {
                throw new IllegalArgumentException("Invalid Stripe customer ID");
            }

            // Payment method ID kontrolü
            String finalPaymentMethodId = paymentMethodId != null ? paymentMethodId
                    : expert.getPaymentInfo().getPaymentMethodId();
            if (finalPaymentMethodId == null || !finalPaymentMethodId.startsWith("pm_")) {
                throw new IllegalArgumentException("Invalid payment method ID");
            }

            // Process the payment and get the PaymentIntent
            PaymentIntent paymentIntent = stripeManager.processSepaPayment(finalStripeCustomerId, finalPaymentMethodId,
                    amount);

            // Create response with necessary client information
            Map<String, Object> response = new HashMap<>();
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("status", paymentIntent.getStatus());
            response.put("requiresAction",
                    paymentIntent.getStatus().equals("requires_action") ||
                            paymentIntent.getStatus().equals("requires_confirmation"));
            response.put("nextAction", paymentIntent.getNextAction());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("Stripe payment failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BusinessException("Payment failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BusinessException("Payment processing failed: " + e.getMessage()));
        }
    }

    // Get payment receipt URL
    @GetMapping("/receipt/{paymentIntentId}")
    public ResponseEntity<?> getPaymentReceiptUrl(@PathVariable String paymentIntentId) {
        try {
            String receiptUrl = stripeManager.retrievePaymentReceiptUrl(paymentIntentId);
            return ResponseEntity.ok(Map.of("receiptUrl", receiptUrl));
        } catch (StripeException e) {
            log.error("Failed to retrieve receipt URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve receipt URL: " + e.getMessage()));
        }
    }

    // Delete Stripe customer
    @DeleteMapping("/customers/{customerId}")
    public ResponseEntity<?> deleteStripeCustomer(@PathVariable String customerId) {
        try {
            stripeManager.deleteStripeCustomer(customerId);
            return ResponseEntity.ok(Map.of("message", "Customer deleted successfully"));
        } catch (BusinessException e) {
            log.error("Failed to delete Stripe customer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Create and confirm payment
    @PostMapping("/create-confirm")
    public ResponseEntity<?> createAndConfirmPayment(@RequestBody Map<String, Object> requestBody) {
        try {
            String customerId = (String) requestBody.get("customerId");
            BigDecimal amount = new BigDecimal(requestBody.get("amount").toString());
            String currency = (String) requestBody.get("currency");
            String description = (String) requestBody.get("description");

            String paymentIntentId = stripeManager.createAndConfirmPayment(customerId, amount, currency, description);
            return ResponseEntity.ok(Map.of("paymentIntentId", paymentIntentId));
        } catch (StripeException e) {
            log.error("Failed to create and confirm payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment failed: " + e.getMessage()));
        }
    }

    // @GetMapping("/payment")
    // public String showPaymentPage(Model model) throws StripeException {
    // // Create a PaymentIntent and return the client_secret
    // PaymentIntent paymentIntent =
    // stripeManager.createSepaPaymentIntent("customerId", 100.0); // Example
    // customer ID and amount
    // String clientSecret = paymentIntent.getClientSecret();
    //
    // // Pass the client secret to the Thymeleaf model
    // model.addAttribute("clientSecret", clientSecret);
    //
    // return "payment";
    // }

    @PostMapping("/process-contact-info")
    public ResponseEntity<?> processContactInfoPayment(@RequestBody Map<String, Object> requestBody) {
        try {
            String expertId = (String) requestBody.get("expertId");
            String stripeCustomerId = (String) requestBody.get("stripeCustomerId");
            String paymentMethodId = (String) requestBody.get("paymentMethodId");
            BigDecimal amount = new BigDecimal(requestBody.get("amount").toString());

            log.info("Processing contact info payment. ExpertId: {}, Amount: {}", expertId, amount);

            // Verify the expert exists and has payment method
            Expert expert = expertRepository.findById(expertId)
                    .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

            if (expert.getPaymentInfo() == null) {
                return ResponseEntity.badRequest()
                        .body(new BusinessException("Expert has not set up payment information"));
            }

            // Stripe customer ID kontrolü
            String finalStripeCustomerId = stripeCustomerId != null ? stripeCustomerId : expert.getStripeCustomerId();
            if (finalStripeCustomerId == null || !finalStripeCustomerId.startsWith("cus_")) {
                throw new IllegalArgumentException("Invalid Stripe customer ID");
            }

            // Payment method ID kontrolü
            String finalPaymentMethodId = paymentMethodId != null ? paymentMethodId
                    : expert.getPaymentInfo().getPaymentMethodId();
            if (finalPaymentMethodId == null || !finalPaymentMethodId.startsWith("pm_")) {
                throw new IllegalArgumentException("Invalid payment method ID");
            }

            // Process the payment and get the PaymentIntent
            PaymentIntent paymentIntent = stripeManager.processSepaPayment(finalStripeCustomerId, finalPaymentMethodId,
                    amount);

            // Create response with necessary client information
            Map<String, Object> response = new HashMap<>();
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("status", paymentIntent.getStatus());
            response.put("requiresAction",
                    paymentIntent.getStatus().equals("requires_action") ||
                            paymentIntent.getStatus().equals("requires_confirmation"));
            response.put("success", paymentIntent.getStatus().equals("succeeded"));

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("Stripe payment failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment processing failed: " + e.getMessage()));
        }
    }

    @GetMapping("/payment-intents/{paymentIntentId}/status")
    public ResponseEntity<?> getPaymentIntentStatus(@PathVariable String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = stripeManager.retrievePaymentIntent(paymentIntentId);
            return ResponseEntity.ok(Map.of(
                    "status", paymentIntent.getStatus(),
                    "requiresAction",
                    paymentIntent.getStatus().equals("requires_action") ||
                            paymentIntent.getStatus().equals("requires_confirmation")));
        } catch (StripeException e) {
            log.error("Failed to retrieve payment intent status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve payment status: " + e.getMessage()));
        }
    }

}
