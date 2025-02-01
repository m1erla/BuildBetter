package com.renovatipoint.business.concretes;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.renovatipoint.business.abstracts.InvoiceService;
import com.renovatipoint.dataAccess.abstracts.ExpertRepository;
import com.renovatipoint.dataAccess.abstracts.InvoiceRepository;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import com.renovatipoint.entities.concretes.Expert;
import com.renovatipoint.entities.concretes.Invoice;
import com.renovatipoint.entities.concretes.User;
import com.renovatipoint.enums.PaymentType;
import com.stripe.exception.StripeException;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceManager implements InvoiceService {

    @Value("${invoice.storage.path}")
    private String invoiceStoragePath;

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final StripeManager stripeManager;

    @Autowired
    public InvoiceManager(
            InvoiceRepository invoiceRepository,
            UserRepository userRepository,
            ExpertRepository expertRepository,
            @Lazy StripeManager stripeManager) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.expertRepository = expertRepository;
        this.stripeManager = stripeManager;
    }

    @PostConstruct
    public void init() {
        try {
            createInvoiceDirectory();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create invoice directory", e);
        }
    }

    private void createInvoiceDirectory() throws IOException {
        // Varsayılan dizin
        if (invoiceStoragePath == null || invoiceStoragePath.trim().isEmpty()) {
            invoiceStoragePath = "invoices";
        }

        Path directory = getInvoiceDirectory();
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    private Path getInvoiceDirectory() {
        // Proje kök dizinini al
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, invoiceStoragePath);
    }

    private byte[] generateInvoicePDF(Invoice invoice) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);

        document.open();

        // Logo ve başlık
        Paragraph header = new Paragraph("RENOVATIPOINT");
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(20);
        document.add(header);

        // Fatura detayları
        document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber()));
        document.add(new Paragraph(
                "Date: " + invoice.getDateIssued().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

        // Müşteri bilgileri
        if (invoice.getUser() != null) {
            document.add(new Paragraph("\nBill To:"));
            document.add(new Paragraph(invoice.getUser().getName() + " " + invoice.getUser().getSurname()));
            document.add(new Paragraph(invoice.getUser().getEmail()));
        } else if (invoice.getExpert() != null) {
            document.add(new Paragraph("\nBill To:"));
            document.add(new Paragraph(invoice.getExpert().getName() + " " + invoice.getExpert().getSurname()));
            document.add(new Paragraph(invoice.getExpert().getEmail()));
            if (invoice.getExpert().getCompanyName() != null) {
                document.add(new Paragraph("Company: " + invoice.getExpert().getCompanyName()));
            }
        }

        // Tutar detayları
        document.add(new Paragraph("\nAmount Details:"));
        document.add(new Paragraph("Amount: €" + invoice.getAmount()));
        document.add(new Paragraph("Status: " + (invoice.isPaid() ? "Paid" : "Pending")));

        // Alt bilgi
        Paragraph footer = new Paragraph("\nThank you for your business!");
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    @Override
    public Invoice generateInvoiceForUser(String userId, double amount, String paymentIntentId)
            throws IOException, StripeException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime dateIssued = LocalDateTime.now();

        // PDF oluştur ve kaydet
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setUser(user);
        invoice.setAmount(BigDecimal.valueOf(amount));
        invoice.setDateIssued(dateIssued);
        invoice.setPaid(true);
        invoice.setPaymentMethodId(paymentIntentId);

        // Stripe makbuz URL'sini al
        String receiptUrl = stripeManager.retrievePaymentReceiptUrl(paymentIntentId);
        invoice.setStripeReceiptUrl(receiptUrl);

        // PDF dosyasını oluştur ve kaydet
        byte[] pdfContent = generateInvoicePDF(invoice);
        String fileName = invoiceNumber + ".pdf";
        String filePath = saveInvoicePDF(pdfContent, fileName);
        invoice.setFilePath(filePath);

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice generateInvoiceForExpert(String expertId, double amount, String paymentIntentId,
            PaymentType paymentType)
            throws IOException, StripeException {
        if (expertId == null || paymentIntentId == null || paymentType == null) {
            throw new IllegalArgumentException("Expert ID, Payment Intent ID and Payment Type cannot be null");
        }

        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new EntityNotFoundException("Expert not found with ID: " + expertId));

        String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime dateIssued = LocalDateTime.now();

        // PDF oluştur ve kaydet
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .expert(expert)
                .user(null) // Explicitly set user to null for expert invoices
                .amount(BigDecimal.valueOf(amount))
                .dateIssued(dateIssued)
                .paid(true)
                .paymentMethodId(paymentIntentId)
                .paymentType(paymentType)
                .build();

        // Stripe makbuz URL'sini al
        String receiptUrl = stripeManager.retrievePaymentReceiptUrl(paymentIntentId);
        invoice.setStripeReceiptUrl(receiptUrl);

        // PDF dosyasını oluştur ve kaydet
        byte[] pdfContent = generateInvoicePDF(invoice);
        String fileName = invoiceNumber + ".pdf";
        String filePath = saveInvoicePDF(pdfContent, fileName);
        invoice.setFilePath(filePath);

        return invoiceRepository.save(invoice);
    }

    private String saveInvoicePDF(byte[] content, String fileName) throws IOException {
        Path directory = getInvoiceDirectory();
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path filePath = directory.resolve(fileName);
        Files.write(filePath, content);

        // Göreceli yol yerine tam yolu döndür
        return filePath.toAbsolutePath().toString();
    }

    @Override
    public byte[] getInvoicePDF(String invoiceId) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        Path path = Paths.get(invoice.getFilePath());
        if (!Files.exists(path)) {
            // Eğer dosya yoksa yeniden oluştur
            byte[] pdfContent = generateInvoicePDF(invoice);
            String fileName = invoice.getInvoiceNumber() + ".pdf";
            String filePath = saveInvoicePDF(pdfContent, fileName);
            invoice.setFilePath(filePath);
            invoiceRepository.save(invoice);
            return pdfContent;
        }

        return Files.readAllBytes(path);
    }

    @Override
    public List<Invoice> findByUserId(String userId) {
        return invoiceRepository.findByUserId(userId);
    }

    @Override
    public List<Invoice> findByExpertId(String expertId) {
        return invoiceRepository.findByExpertId(expertId);
    }

    @Override
    public Optional<Invoice> findById(String invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    @Override
    public Invoice findByPaymentMethodId(String paymentMethodId) {
        return invoiceRepository.findByPaymentMethodId(paymentMethodId);
    }

    @Override
    public Invoice findByStripeCustomerId(String customerId) {
        return invoiceRepository.findByStripeCustomerId(customerId);
    }

    @Override
    public void save(Invoice invoice) {
        invoiceRepository.save(invoice);
    }
}
