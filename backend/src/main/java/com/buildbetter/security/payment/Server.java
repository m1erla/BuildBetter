package com.buildbetter.security.payment;

import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Payout;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PayoutCreateParams;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Paths;
import java.util.HashMap;

import static spark.Spark.*;

public class Server {
    private static Gson gson = new Gson();

    static class ConfigResponse {
        private String publishableKey;

        public ConfigResponse(String publishableKey) {
            this.publishableKey = publishableKey;
        }
    }

    static class FailureResponse {
        private HashMap<String, String> error;

        public FailureResponse(String message) {
            this.error = new HashMap<String, String>();
            this.error.put("message", message);
        }
    }

    static class CreatePaymentResponse {
        private String clientSecret;

        public CreatePaymentResponse(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    public static void main(String[] args) {
        port(4242);
        Dotenv dotenv = Dotenv.load();

        Stripe.apiKey = dotenv.get("STRIPE_SECRET_KEY");

        // For sample support and debugging, not required for production:
        Stripe.setAppInfo(
                "stripe-samples/accept-a-payment/payment-element",
                "0.0.1",
                "https://github.com/stripe-samples"
        );

        staticFiles.externalLocation(
                Paths.get(
                        Paths.get("").toAbsolutePath().toString(),
                        dotenv.get("STATIC_DIR")
                ).normalize().toString());

        get("/config", (request, response) -> {
            response.type("application/json");

            return gson.toJson(new ConfigResponse(dotenv.get("STRIPE_PUBLISHABLE_KEY")));
        });

        get("/create-payment-intent", (request, response) -> {
            response.type("application/json");

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .setCurrency("EUR")
                    .setAmount(1999L)
                    .build();

            try {
                // Create a PaymentIntent with the order amount and currency
                PaymentIntent intent = PaymentIntent.create(params);

                // Send PaymentIntent details to client
                return gson.toJson(new CreatePaymentResponse(intent.getClientSecret()));
            } catch(StripeException e) {
                response.status(400);
                return gson.toJson(new FailureResponse(e.getMessage()));
            } catch(Exception e) {
                response.status(500);
                return gson.toJson(e);
            }
        });
        post("/payouts" ,(request, response)-> {
            response.type("application/json");

            PayoutCreateParams params =
                    PayoutCreateParams.builder().setAmount(5000L).setCurrency("eur").build();

            Payout payout = Payout.create(params);
            return gson.toJson(payout);
        });
        post("/webhook", (request, response) -> {
            String payload = request.body();
            String sigHeader = request.headers("Stripe-Signature");
            String endpointSecret = dotenv.get("STRIPE_WEBHOOK_SECRET");

            Event event = null;

            try {
                event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            } catch (SignatureVerificationException e) {
                // Invalid signature
                response.status(400);
                return "";
            }

            switch (event.getType()) {
                case "payment_intent.succeeded":
                    // Fulfill any orders, e-mail receipts, etc
                    // To cancel the payment you will need to issue a Refund
                    // (https://stripe.com/docs/api/refunds)
                    System.out.println("💰Payment received!");
                    break;
                case "payment_intent.payment_failed":
                    System.out.println("❌ Payment failed.");
                    break;
                default:
                    // Unexpected event type
                    response.status(400);
                    return "";
            }

            response.status(200);
            return "";
        });
    }
}
