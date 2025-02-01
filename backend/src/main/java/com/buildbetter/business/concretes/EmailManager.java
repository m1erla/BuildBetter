package com.renovatipoint.business.concretes;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailManager {

    private final JavaMailSender emailSender;

    public EmailManager(JavaMailSender emailSender){
        this.emailSender = emailSender;
    }

    public void sendSimpleMessage(String to, String subject, String text){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("legitimatebusiness007@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    public void sendPaymentConfirmation(String to, String transactionId, BigDecimal amount){
        String subject = "Payment Confirmation";
        String text = String.format("Your payment of €%.2f has been processed successfully. Transaction ID: #s", amount, transactionId);
        sendSimpleMessage(to, subject, text);
    }

    public void sendPaymentFailure(String to, String transactionId, BigDecimal amount)
    {
        String subject = "Payment Failed";
        String text = String.format("Your payment of €%.2f has failed. Please check your payment method. Transaction ID: #s", amount, transactionId);
        sendSimpleMessage(to, subject, text);
    }

    public void sendInformationSharingNotification(String to, String expertName){
        String subject = "Information Shared with Expert";
        String text = String.format("Your information has been shared with expert %s. A fee of €1.00 has been charged.", expertName);
        sendSimpleMessage(to, subject, text);

    }

    public void sendJobCompletionNotification(String to, String adId){
        String subject = "Job Completed";
        String text = String.format("The job (ID: %s) has been marked as completed. A fee of €5.00 has been charged.", adId);
        sendSimpleMessage(to, subject, text);

    }
}
