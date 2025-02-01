package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.ExpertService;
import com.renovatipoint.business.abstracts.UserService;
import com.renovatipoint.business.concretes.ExpertManager;
import com.renovatipoint.business.concretes.StripeManager;
import com.renovatipoint.business.requests.SepaPaymentRequest;
import com.renovatipoint.business.requests.UpdateExpertRequest;
import com.renovatipoint.business.responses.GetExpertResponse;
import com.renovatipoint.dataAccess.abstracts.ExpertRepository;
import com.renovatipoint.dataAccess.abstracts.PaymentInfoRepository;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import com.renovatipoint.entities.concretes.Expert;
import com.renovatipoint.entities.concretes.PaymentInfo;
import com.renovatipoint.security.jwt.JwtService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PaymentMethodListParams;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/experts")
@Slf4j
public class ExpertController {

    private final ExpertService expertService;

    private final ExpertRepository expertRepository;
    private final UserRepository userRepository;
    private final ExpertManager expertManager;
    private final UserService userService;

    private final JwtService jwtService;

    private final StripeManager stripeManager;

    private final PaymentInfoRepository paymentInfoRepository;

    public ExpertController(ExpertService expertService, ExpertRepository expertRepository,
            UserRepository userRepository, ExpertManager expertManager, UserService userService, JwtService jwtService,
            StripeManager stripeManager, PaymentInfoRepository paymentInfoRepository) {
        this.expertService = expertService;
        this.expertRepository = expertRepository;
        this.userRepository = userRepository;
        this.expertManager = expertManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.stripeManager = stripeManager;
        this.paymentInfoRepository = paymentInfoRepository;
    }

    @GetMapping("/{expertId}/payment-info")
    public ResponseEntity<?> getExpertPaymentInfo(@PathVariable String expertId) {
        try {
            Expert expert = expertRepository.findById(expertId)
                    .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

            PaymentInfo paymentInfo = expert.getPaymentInfo();

            // Eğer payment info yoksa veya gerekli alanlar eksikse
            if (paymentInfo == null ||
                    paymentInfo.getIban() == null ||
                    paymentInfo.getBic() == null ||
                    paymentInfo.getBankName() == null ||
                    paymentInfo.getPaymentMethodId() == null) {
                return ResponseEntity.ok(Map.of(
                        "hasPaymentSetup", false,
                        "stripeCustomerId", expert.getStripeCustomerId(),
                        "message", "Expert has not completed payment setup"));
            }

            // Tüm gerekli bilgiler varsa
            return ResponseEntity.ok(Map.of(
                    "hasPaymentSetup", true,
                    "stripeCustomerId", expert.getStripeCustomerId(),
                    "paymentMethodId", paymentInfo.getPaymentMethodId(),
                    "iban", paymentInfo.getIban(),
                    "bic", paymentInfo.getBic(),
                    "bankName", paymentInfo.getBankName(),
                    "lastFourDigits", paymentInfo.getIban().substring(paymentInfo.getIban().length() - 4)));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch expert payment information"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetExpertResponse> getExpertById(@PathVariable String id) {
        GetExpertResponse expert = expertService.getExpertById(id);

        return ResponseEntity.ok(expert);
    }

    @GetMapping("/responseExpert")
    public ResponseEntity<GetExpertResponse> retrieveExpertProfileWithResponse(
            @RequestHeader("Authorization") String authorizationHeader) {
        // Extract the token from the Authorization header (remove "Bearer " prefix)
        String jwt = authorizationHeader.substring(7).trim();

        String email = jwtService.extractUsername(jwt);
        return ResponseEntity.ok(userService.getExpertByEmail(email));
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@ModelAttribute UpdateExpertRequest updateExpertRequest) throws IOException {
        try {
            // Token'dan uzman bilgilerini al
            String expertId = updateExpertRequest.getId();
            if (expertId == null || expertId.isEmpty()) {
                return ResponseEntity.badRequest().body("Expert ID is required");
            }

            return expertManager.update(updateExpertRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating expert: " + e.getMessage());
        }
    }

    @PostMapping("/setup-sepa")
    public ResponseEntity<?> setupSepaPaymentMethod(@RequestBody SepaPaymentRequest request, Principal principal)
            throws StripeException {
        Expert expert = expertRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Expert not found!"));

        try {
            // Validate Stripe customer ID
            if (expert.getStripeCustomerId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Expert's Stripe customer ID is missing. Please contact support."));
            }

            // Önce mevcut payment method'ları kontrol et ve varsa sil
            PaymentMethodListParams listParams = PaymentMethodListParams.builder()
                    .setCustomer(expert.getStripeCustomerId())
                    .setType(PaymentMethodListParams.Type.SEPA_DEBIT)
                    .build();

            PaymentMethodCollection existingMethods = PaymentMethod.list(listParams);
            for (PaymentMethod method : existingMethods.getData()) {
                method.detach();
            }

            // Create a SEPA Payment Method
            PaymentMethodCreateParams createParams = PaymentMethodCreateParams.builder()
                    .setType(PaymentMethodCreateParams.Type.SEPA_DEBIT)
                    .setSepaDebit(
                            PaymentMethodCreateParams.SepaDebit.builder()
                                    .setIban(request.getIban())
                                    .build())
                    .setBillingDetails(
                            PaymentMethodCreateParams.BillingDetails.builder()
                                    .setName(expert.getName())
                                    .setEmail(expert.getEmail())
                                    .build())
                    .build();

            PaymentMethod paymentMethod = PaymentMethod.create(createParams);

            // Attach the payment method to the Stripe customer
            PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
                    .setCustomer(expert.getStripeCustomerId())
                    .build();

            paymentMethod = paymentMethod.attach(attachParams);

            // Save payment info in the database
            PaymentInfo paymentInfo = expert.getPaymentInfo();
            if (paymentInfo == null) {
                paymentInfo = new PaymentInfo();
            }

            paymentInfo.setIban(request.getIban());
            paymentInfo.setBic(request.getBic());
            paymentInfo.setBankName(request.getBankName());
            paymentInfo.setStripeCustomerId(expert.getStripeCustomerId());
            paymentInfo.setPaymentMethodId(paymentMethod.getId());
            paymentInfo.setExpert(expert);

            expert.setPaymentInfo(paymentInfo);
            paymentInfoRepository.save(paymentInfo);
            expertRepository.save(expert);

            // Return customerId and paymentMethodId for frontend storage
            Map<String, Object> response = new HashMap<>();
            response.put("customerId", expert.getStripeCustomerId());
            response.put("paymentMethodId", paymentMethod.getId());
            response.put("success", true);
            response.put("message", "SEPA payment method setup completed successfully");

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Failed to setup payment method: " + e.getMessage(),
                            "success", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error: " + e.getMessage(),
                            "success", false));
        }
    }

    // Create Stripe customer for expert
    @PostMapping("/create-stripe-customer")
    public ResponseEntity<?> createStripeCustomer(Principal principal) {
        try {
            Expert expert = expertRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

            if (expert.getStripeCustomerId() != null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Expert already has a Stripe customer ID"));
            }

            String stripeCustomerId = stripeManager.createStripeCustomer(expert.getEmail(), expert.getName());
            expert.setStripeCustomerId(stripeCustomerId);
            expertRepository.save(expert);

            return ResponseEntity.ok(Map.of(
                    "stripeCustomerId", stripeCustomerId,
                    "message", "Stripe customer created successfully"));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create Stripe customer: " + e.getMessage()));
        }
    }

    // Attach payment method to customer
    @PostMapping("/attach-payment-method")
    public ResponseEntity<?> attachPaymentMethod(@RequestBody Map<String, String> request, Principal principal) {
        try {
            Expert expert = expertRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

            String paymentMethodId = request.get("paymentMethodId");
            if (paymentMethodId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Payment method ID is required"));
            }

            stripeManager.attachPaymentMethodToCustomer(expert.getStripeCustomerId(), paymentMethodId);
            return ResponseEntity.ok(Map.of("message", "Payment method attached successfully"));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to attach payment method: " + e.getMessage()));
        }
    }

    // Delete payment method
    @DeleteMapping("/{expertId}/payment-method")
    public ResponseEntity<?> deletePaymentMethod(@PathVariable String expertId,
            @RequestBody Map<String, String> request, Principal principal) {
        try {
            // Kullanıcı yetkisini kontrol et
            Expert expert = expertRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

            if (!expert.getId().equals(expertId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized access"));
            }

            String type = request.get("type");
            if (type == null || !type.equals("BANK")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid payment method type"));
            }

            PaymentInfo paymentInfo = expert.getPaymentInfo();
            if (paymentInfo == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No payment information found"));
            }

            // Stripe'dan ödeme yöntemini sil
            if (paymentInfo.getPaymentMethodId() != null) {
                try {
                    PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentInfo.getPaymentMethodId());
                    paymentMethod.detach();
                } catch (StripeException e) {
                    log.error("Error detaching payment method from Stripe: {}", e.getMessage());
                }
            }

            // PaymentInfo'yu sadece deaktive et
            paymentInfoRepository.deactivatePaymentInfo(paymentInfo.getId());

            // Frontend'e güncel durumu bildir
            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "Payment method deleted successfully",
                            "isActive", false));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting payment method: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete payment method: " + e.getMessage()));
        }
    }

}
