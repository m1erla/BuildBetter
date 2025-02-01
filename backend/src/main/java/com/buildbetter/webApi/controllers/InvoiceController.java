package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.InvoiceService;
import com.renovatipoint.entities.concretes.Invoice;
import com.renovatipoint.enums.PaymentType;
import com.stripe.exception.StripeException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invoices")
@CrossOrigin
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Invoice>> getUserInvoices(@PathVariable String userId) {
        try {
            List<Invoice> invoices = invoiceService.findByUserId(userId);
            if (invoices.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/expert/{expertId}")
    public ResponseEntity<List<Invoice>> getExpertInvoices(@PathVariable String expertId) {
        try {
            List<Invoice> invoices = invoiceService.findByExpertId(expertId);
            if (invoices.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{invoiceId}")
    public ResponseEntity<?> downloadInvoice(@PathVariable String invoiceId) {
        try {
            byte[] pdfContent = invoiceService.getInvoicePDF(invoiceId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "invoice-" + invoiceId + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error downloading invoice: " + e.getMessage());
        }
    }

    @GetMapping("/receipt/{invoiceId}")
    public ResponseEntity<?> getInvoiceReceiptUrl(@PathVariable String invoiceId) {
        try {
            Invoice invoice = invoiceService.findById(invoiceId)
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

            if (invoice.getStripeReceiptUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(invoice.getStripeReceiptUrl());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving receipt URL: " + e.getMessage());
        }
    }

    @PostMapping("/generate/user")
    public ResponseEntity<?> generateUserInvoice(
            @RequestParam String userId,
            @RequestParam double amount,
            @RequestParam String paymentIntentId) {
        try {
            Invoice invoice = invoiceService.generateInvoiceForUser(userId, amount, paymentIntentId);
            return ResponseEntity.ok(invoice);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException | StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating invoice: " + e.getMessage());
        }
    }

    @PostMapping("/generate/expert")
    public ResponseEntity<?> generateExpertInvoice(@RequestBody Map<String, Object> request) {
        try {
            String expertId = (String) request.get("expertId");
            double amount = ((Number) request.get("amount")).doubleValue();
            String paymentIntentId = (String) request.get("paymentIntentId");
            String paymentTypeStr = (String) request.get("paymentType");

            if (expertId == null || paymentIntentId == null || paymentTypeStr == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            PaymentType paymentType;
            try {
                paymentType = PaymentType.valueOf(paymentTypeStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid payment type: " + paymentTypeStr);
            }

            Invoice invoice = invoiceService.generateInvoiceForExpert(expertId, amount, paymentIntentId, paymentType);
            return ResponseEntity.ok(invoice);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException | StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating invoice: " + e.getMessage());
        }
    }
}
