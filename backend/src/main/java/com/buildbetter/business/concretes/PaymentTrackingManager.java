package com.renovatipoint.business.concretes;

import com.renovatipoint.dataAccess.abstracts.ExpertRepository;
import com.renovatipoint.entities.concretes.Expert;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentTrackingManager {

    private ExpertRepository expertRepository;

    private JavaMailSender mailSender;

    private StripeManager stripeManager;
    @Scheduled(cron = "0 0 1 * * ?")
    public void trackPayments(){
        List<Expert> expertWithPaymentIssues = expertRepository.findAllWithPaymentIssues();
        for (Expert expert : expertWithPaymentIssues){
            sendWarningEmail(expert);
            blockUserIfNeeded(expert);
        }
    }

    private void sendWarningEmail(Expert expert){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("renovatipoint@gmail.com");
        message.setTo(expert.getEmail());
        message.setSubject("Payment Issue Warning");
        message.setText("Dear " + expert.getName() + ",\n\nYou have a pending payment issue. Please resolve it to avoid account blocking.\n\nThank you!");

        mailSender.send(message);
    }

    private void blockUserIfNeeded(Expert expert){
        if (expert.getPaymentIssuesCount() >= 2){
            expert.setAccountBlocked(true);
            expertRepository.save(expert);
        }
    }
}
