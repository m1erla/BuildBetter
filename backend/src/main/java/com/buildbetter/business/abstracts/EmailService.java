package com.buildbetter.business.abstracts;

import com.buildbetter.enums.EmailTemplateType;

import java.util.Map;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
    void sendHtmlEmail(String to, String subject, String htmlBody);
    void sendTemplateEmail(String to, EmailTemplateType templateType, Map<String, Object> variables);

    // Common email types
    void sendWelcomeEmail(String to, String userName);
    void sendEmailVerification(String to, String verificationToken);
    void sendPasswordReset(String to, String resetToken);
    void sendPasswordChanged(String to);
    void sendPaymentSuccess(String to, String amount, String invoiceUrl);
    void sendPaymentFailed(String to, String amount, String reason);
    void sendSubscriptionCreated(String to, String planName);
    void sendTrialEnding(String to, int daysLeft);
    void sendInvoiceReady(String to, String invoiceId, String downloadUrl);
}
