//package com.renovatipoint.business.concretes;
//
//import com.renovatipoint.core.utilities.exceptions.BusinessException;
//import com.renovatipoint.dataAccess.abstracts.ExpertRepository;
//import com.renovatipoint.dataAccess.abstracts.OperationRepository;
//import com.renovatipoint.dataAccess.abstracts.UserRepository;
//import com.renovatipoint.entities.concretes.Expert;
//import com.renovatipoint.entities.concretes.Operation;
//import com.renovatipoint.entities.concretes.User;
//import com.renovatipoint.enums.OperationStatus;
//import com.renovatipoint.enums.OperationType;
//import com.stripe.exception.StripeException;
//import com.stripe.model.PaymentIntent;
//import jakarta.persistence.EntityNotFoundException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@Service
//public class OperationBusinessManager {
//
//    private final OperationRepository operationRepository;
//    private final UserRepository userRepository;
//    private final ExpertRepository expertRepository;
//    private final StripeManager stripeManager;
//    private final EmailManager emailService;
//
//    @Autowired
//    public OperationBusinessManager(OperationRepository operationRepository,
//                                    UserRepository userRepository,
//                                    ExpertRepository expertRepository,
//                                    StripeManager stripeManager,
//                                    EmailManager emailService) {
//        this.operationRepository = operationRepository;
//        this.userRepository = userRepository;
//        this.expertRepository = expertRepository;
//        this.stripeManager = stripeManager;
//        this.emailService = emailService;
//    }
//
//    @Transactional
//    public Operation createInformationSharingTransaction(String expertId, String userId) throws StripeException, BusinessException {
//        Expert expert = expertRepository.findById(expertId)
//                .orElseThrow(() -> new EntityNotFoundException("Expert not found"));
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        BigDecimal amount = BigDecimal.valueOf(1.00);
//        if (expert.getBalance().compareTo(amount) < 0) {
//            throw new BusinessException("Expert does not have sufficient balance");
//        }
//
//        try {
//            PaymentIntent paymentIntent = stripeManager.createSepaPaymentIntent(
//                    expert.getStripeCustomerId(),
//                    expert.getPaymentInfo().getPaymentMethodId(),
//                    amount.doubleValue()
//            );
//
//            Operation operation = Operation.builder()
//                    .expert(expert)
//                    .user(user)
//                    .amount(amount)
//                    .type(OperationType.INFORMATION_SHARING)
//                    .status(OperationStatus.COMPLETED)
//                    .paymentMethodId(paymentIntent.getId())
//                    .build();
//
//            expert.setBalance(expert.getBalance().subtract(amount));
//            expertRepository.save(expert);
//
//            Operation savedOperation = operationRepository.save(operation);
//
//            // Send email notifications
//            emailService.sendPaymentConfirmation(expert.getEmail(), savedOperation.getId(), BigDecimal.valueOf(amount.doubleValue()));
//            emailService.sendInformationSharingNotification(user.getEmail(), expert.getName());
//
//            return savedOperation;
//        } catch (StripeException e) {
//            emailService.sendPaymentFailure(expert.getEmail(), "N/A", BigDecimal.valueOf(amount.doubleValue()));
//            throw e;
//        }
//    }
//
//    @Transactional
//    public Operation createJobCompletionTransaction(String expertId, String userId, String adId) throws StripeException, BusinessException {
//        Expert expert = expertRepository.findById(expertId)
//                .orElseThrow(() -> new EntityNotFoundException("Expert not found"));
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        BigDecimal amount = BigDecimal.valueOf(5.00);
//        if (expert.getBalance().compareTo(amount) < 0) {
//            throw new BusinessException("Expert does not have sufficient balance");
//        }
//
//        try {
//            PaymentIntent paymentIntent = stripeManager.createSepaPaymentIntent(
//                    expert.getStripeCustomerId(),
//                    expert.getPaymentInfo().getPaymentMethodId(),
//                    amount.doubleValue()
//            );
//
//            Operation operation = Operation.builder()
//                    .expert(expert)
//                    .user(user)
//                    .amount(amount)
//                    .type(OperationType.JOB_COMPLETION)
//                    .status(OperationStatus.COMPLETED)
//                    .paymentMethodId(paymentIntent.getId())
//                    .build();
//
//            expert.setBalance(expert.getBalance().subtract(amount));
//            expertRepository.save(expert);
//
//            Operation savedOperation = operationRepository.save(operation);
//
//            // Send email notifications
//            emailService.sendPaymentConfirmation(expert.getEmail(), savedOperation.getId(), BigDecimal.valueOf(amount.doubleValue()));
//            emailService.sendJobCompletionNotification(user.getEmail(), adId);
//
//            return savedOperation;
//        } catch (StripeException e) {
//            emailService.sendPaymentFailure(expert.getEmail(), "N/A", BigDecimal.valueOf(amount.doubleValue()));
//            throw e;
//        }
//    }
//
//    public List<Operation> getTransactionsByExpert(String expertId){
//        return  operationRepository.findByExpertId(expertId);
//    }
//
//    public List<Operation> getTransactionsByUser(String userId){
//        return operationRepository.findByUserId(userId);
//    }
//
//    public List<Operation> getTransactionsByType(OperationType type){
//        return operationRepository.findByType(type);
//    }
//
////    public List<Operation> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end){
////        return operationRepository.findByCreatedAtBetween(start, end);
////    }
//
//
//}
