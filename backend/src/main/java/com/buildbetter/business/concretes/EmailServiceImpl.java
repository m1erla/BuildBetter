package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.EmailService;
import com.buildbetter.dataAccess.abstracts.EmailTemplateRepository;
import com.buildbetter.entities.concretes.EmailTemplate;
import com.buildbetter.enums.EmailTemplateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRepository templateRepository;

    @Value("${spring.mail.username:noreply@buildbetter.com}")
    private String fromEmail;

    @Value("${app.name:BuildBetter}")
    private String appName;

    @Value("${app.url:https://buildbetter.com}")
    private String appUrl;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    @Override
    public void sendTemplateEmail(String to, EmailTemplateType templateType, Map<String, Object> variables) {
        EmailTemplate template = templateRepository.findByType(templateType)
                .orElseThrow(() -> new RuntimeException("Email template not found: " + templateType));

        String subject = processTemplate(template.getSubject(), variables);
        String htmlContent = processTemplate(template.getHtmlContent(), variables);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendWelcomeEmail(String to, String userName) {
        Map<String, Object> variables = Map.of(
                "userName", userName,
                "appName", appName,
                "appUrl", appUrl
        );
        sendTemplateEmail(to, EmailTemplateType.WELCOME, variables);
    }

    @Override
    public void sendEmailVerification(String to, String verificationToken) {
        String verificationUrl = appUrl + "/verify-email?token=" + verificationToken;
        Map<String, Object> variables = Map.of(
                "verificationUrl", verificationUrl,
                "appName", appName
        );
        sendTemplateEmail(to, EmailTemplateType.EMAIL_VERIFICATION, variables);
    }

    @Override
    public void sendPasswordReset(String to, String resetToken) {
        String resetUrl = appUrl + "/reset-password?token=" + resetToken;
        Map<String, Object> variables = Map.of(
                "resetUrl", resetUrl,
                "appName", appName
        );
        sendTemplateEmail(to, EmailTemplateType.PASSWORD_RESET, variables);
    }

    @Override
    public void sendPasswordChanged(String to) {
        Map<String, Object> variables = Map.of("appName", appName);
        sendTemplateEmail(to, EmailTemplateType.PASSWORD_CHANGED, variables);
    }

    @Override
    public void sendPaymentSuccess(String to, String amount, String invoiceUrl) {
        Map<String, Object> variables = Map.of(
                "amount", amount,
                "invoiceUrl", invoiceUrl,
                "appName", appName
        );
        sendTemplateEmail(to, EmailTemplateType.PAYMENT_SUCCESS, variables);
    }

    @Override
    public void sendPaymentFailed(String to, String amount, String reason) {
        Map<String, Object> variables = Map.of(
                "amount", amount,
                "reason", reason,
                "appName", appName,
                "supportUrl", appUrl + "/support"
        );
        sendTemplateEmail(to, EmailTemplateType.PAYMENT_FAILED, variables);
    }

    @Override
    public void sendSubscriptionCreated(String to, String planName) {
        Map<String, Object> variables = Map.of(
                "planName", planName,
                "appName", appName,
                "dashboardUrl", appUrl + "/dashboard"
        );
        sendTemplateEmail(to, EmailTemplateType.SUBSCRIPTION_CREATED, variables);
    }

    @Override
    public void sendTrialEnding(String to, int daysLeft) {
        Map<String, Object> variables = Map.of(
                "daysLeft", String.valueOf(daysLeft),
                "appName", appName,
                "upgradeUrl", appUrl + "/billing/upgrade"
        );
        sendTemplateEmail(to, EmailTemplateType.TRIAL_ENDING, variables);
    }

    @Override
    public void sendInvoiceReady(String to, String invoiceId, String downloadUrl) {
        Map<String, Object> variables = Map.of(
                "invoiceId", invoiceId,
                "downloadUrl", downloadUrl,
                "appName", appName
        );
        sendTemplateEmail(to, EmailTemplateType.INVOICE_READY, variables);
    }

    private String processTemplate(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return result;
    }
}
