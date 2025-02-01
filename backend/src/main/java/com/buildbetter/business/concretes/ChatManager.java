package com.renovatipoint.business.concretes;

import com.renovatipoint.business.abstracts.ChatService;
import com.renovatipoint.core.utilities.detector.ContactInfoDetector;
import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.dataAccess.abstracts.*;
import com.renovatipoint.entities.concretes.*;
import com.renovatipoint.enums.ChatRoomStatus;
import com.renovatipoint.enums.MessageType;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@Transactional
public class ChatManager implements ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ContactSharingRecordRepository contactSharingRecordRepository;
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final NotificationManager notificationManager;
    private final StripeManager stripeManager;
    private final InvoiceManager invoiceManager;

    @Autowired
    public ChatManager(ChatRoomRepository chatRoomRepository, ChatMessageRepository chatMessageRepository,
            ContactSharingRecordRepository contactSharingRecordRepository, UserRepository userRepository,
            ExpertRepository expertRepository, NotificationManager notificationManager,
            @Lazy StripeManager stripeManager, @Lazy InvoiceManager invoiceManager) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.contactSharingRecordRepository = contactSharingRecordRepository;
        this.userRepository = userRepository;
        this.expertRepository = expertRepository;
        this.notificationManager = notificationManager;
        this.stripeManager = stripeManager;
        this.invoiceManager = invoiceManager;
    }

    public ChatRoom getChatRoomWithValidation(String chatRoomId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        if (!canAccessChatRoom(user, chatRoom)) {
            throw new AccessDeniedException("You don't have access to this chat room");
        }

        if (user instanceof Expert && chatRoom.isExpertBlocked()) {
            throw new IllegalStateException("Expert account is blocked");
        }

        if (chatRoom.getStatus() == ChatRoomStatus.COMPLETED ||
                chatRoom.isCompleted() ||
                chatRoom.isCompletionPaymentProcessed()) {
            chatRoom.setCompleted(true);
            chatRoom.setStatus(ChatRoomStatus.COMPLETED);
            chatRoom.setCompletionPaymentProcessed(true);
            chatRoomRepository.save(chatRoom);
        }

        return chatRoom;
    }

    @Override
    public Page<ChatMessage> getChatMessages(String chatRoomId, String userEmail, Pageable pageable) {
        ChatRoom chatRoom = getChatRoomWithValidation(chatRoomId, userEmail);
        return chatMessageRepository.findByChatRoomId(chatRoomId, pageable);
    }

    @Override
    @Transactional
    public ChatMessage sendMessage(String senderId, String chatRoomId, String content, boolean isContactInfo)
            throws StripeException {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Sender not found"));

        // Add validation for completed chat rooms
        if (chatRoom.isCompleted()) {
            throw new BusinessException("Cannot send messages in a completed chat room");
        }

        if (!canAccessChatRoom(sender, chatRoom)) {
            throw new AccessDeniedException("User does not have access to this chat room");
        }

        boolean containsContactInfo = false;
        if (!(sender instanceof Expert) &&
                (isContactInfo || ContactInfoDetector.containsContactInformation(content))) {
            containsContactInfo = true;
            handleContactInformationSharing(chatRoom.getExpert(), sender, chatRoom, true);
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(content)
                .messageType(determineMessageType(content, containsContactInfo, senderId))
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .contactInfo(containsContactInfo)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Update chat room activity
        chatRoom.setLastActivity(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // Notify recipient
        String recipientId = determineRecipientId(sender, chatRoom);
        notificationManager.notifyNewMessage(recipientId, sender.getId(), chatRoom.getId(), content);

        return savedMessage;
    }

    @Override
    @Transactional
    public void handleContactInformationSharing(Expert expert, User user, ChatRoom chatRoom,
            boolean isPendingPayment) throws StripeException {
        // First, verify the chat room is active
        if (chatRoom.getStatus() != ChatRoomStatus.ACTIVE) {
            throw new BusinessException("Cannot share contact information in a " + chatRoom.getStatus() + " chat room");
        }

        // Check if expert is blocked
        if (expert.isAccountBlocked()) {
            throw new IllegalStateException("Expert account is blocked");
        }

        // Check if contact information has already been shared using multiple criteria
        if (chatRoom.isContactShared()) {
            throw new BusinessException("Contact information has already been shared in this chat room");
        }

        if (chatRoomRepository.hasContactBeenShared(chatRoom.getId())) {
            throw new BusinessException("Contact information has already been shared for this chat room");
        }

        if (chatMessageRepository.existsByChatRoomIdAndMessageType(chatRoom.getId(), MessageType.CONTACT_INFO)) {
            throw new BusinessException("Contact information has already been shared via messages");
        }

        try {
            // Handle payment if not pending
            if (!isPendingPayment) {
                BigDecimal contactInfoCharge = new BigDecimal("1.00");

                // Process Stripe payment
                String paymentIntentId = stripeManager.createAndConfirmPayment(
                        expert.getStripeCustomerId(),
                        contactInfoCharge,
                        "EUR",
                        "Contact information sharing fee for ad: " + chatRoom.getAd().getTitle());

                // Record the sharing
                ContactSharingRecord sharingRecord = ContactSharingRecord.builder()
                        .user(user)
                        .expert(expert)
                        .chatRoom(chatRoom)
                        .ad(chatRoom.getAd())
                        .paymentProcessed(true)
                        .stripePaymentIntentId(paymentIntentId)
                        .sharedAt(LocalDateTime.now())
                        .build();

                contactSharingRecordRepository.save(sharingRecord);

                // Update chat room status
                chatRoom.setContactShared(true);
                chatRoom.setContactSharedAt(LocalDateTime.now());
                chatRoomRepository.save(chatRoom);

                // Create system message for contact sharing
                ChatMessage systemMessage = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .content("Contact information has been shared. Expert has been charged €1.")
                        .messageType(MessageType.SYSTEM)
                        .timestamp(LocalDateTime.now())
                        .isRead(true)
                        .build();
                chatMessageRepository.save(systemMessage);
            }
        } catch (Exception e) {
            log.error("Error processing contact information sharing", e);
            throw new BusinessException("Failed to process contact sharing: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void markJobAsComplete(String chatRoomId, String expertId) throws StripeException {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

        // Validate expert
        if (!chatRoom.getExpert().getId().equals(expertId)) {
            throw new AccessDeniedException("Only the assigned expert can complete this job");
        }

        // Validate chat room status
        if (chatRoom.getStatus() != ChatRoomStatus.ACTIVE) {
            throw new BusinessException("This chat room is " + chatRoom.getStatus() + " and cannot be completed");
        }

        if (chatRoom.isCompleted()) {
            throw new BusinessException("This job has already been completed");
        }

        // Check if payment has already been processed
        if (chatRoom.getStripePaymentIntentId() != null) {
            try {
                // Verify the payment status
                PaymentIntent paymentIntent = stripeManager.retrievePaymentIntent(chatRoom.getStripePaymentIntentId());
                if ("succeeded".equals(paymentIntent.getStatus())) {
                    // Payment already processed, just update the status
                    updateChatRoomStatus(chatRoom, expert, chatRoom.getStripePaymentIntentId());
                    return;
                }
            } catch (StripeException e) {
                log.error("Error verifying existing payment", e);
            }
        }

        // Update chat room status
        updateChatRoomStatus(chatRoom, expert, chatRoom.getStripePaymentIntentId());
    }

    private void updateChatRoomStatus(ChatRoom chatRoom, Expert expert, String paymentIntentId) {
        try {
            // Update chat room status
            chatRoom.setStatus(ChatRoomStatus.COMPLETED);
            chatRoom.setCompletedAt(LocalDateTime.now());
            chatRoom.setCompletionPaymentProcessed(true);
            if (paymentIntentId != null) {
                chatRoom.setStripePaymentIntentId(paymentIntentId);
            }
            chatRoom.setCompleted(true);
            chatRoom.setActive(false);
            chatRoomRepository.save(chatRoom);

            // Create system message for completion
            ChatMessage completionMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .content("Job marked as completed by expert. Chat room is now closed.")
                    .messageType(MessageType.SYSTEM)
                    .timestamp(LocalDateTime.now())
                    .isRead(true)
                    .sender(expert)
                    .build();
            chatMessageRepository.save(completionMessage);

            // Notify participants
            notificationManager.notifyJobCompleted(
                    chatRoom.getUser().getId(),
                    chatRoom.getExpert().getId(),
                    chatRoom.getAd().getTitle(),
                    chatRoom.getId());

        } catch (Exception e) {
            log.error("Error updating chat room status", e);
            throw new BusinessException("Failed to update chat room status: " + e.getMessage());
        }
    }

    // Chat Room Management
    @Override
    @Transactional
    public ChatRoom createChatRoomForAcceptedRequest(Request request) {
        log.debug("Creating chat room for accepted request: {}", request.getId());
        // Verify expert account status
        if (request.getExpert().isAccountBlocked()) {
            throw new IllegalStateException("Cannot create chat room: Expert account is blocked");
        }

        // Check if chat room already exists
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByRequest(request);
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        // Create chat room
        ChatRoom chatRoom = ChatRoom.builder()
                .chatId(UUID.randomUUID().toString())
                .user(request.getAd().getUser())
                .expert(request.getExpert())
                .ad(request.getAd())
                .request(request)
                .active(true)
                .expertBlocked(request.getExpert().isAccountBlocked())
                .lastActivity(LocalDateTime.now())
                .status(ChatRoomStatus.ACTIVE)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // Create welcome message
        ChatMessage welcomeMessage = ChatMessage.builder()
                .chatRoom(savedChatRoom)
                .content(String.format("Chat room created for ad: %s. Conversation between %s and %s",
                        request.getAd().getTitle(),
                        request.getUser().getName(),
                        request.getExpert().getName()))
                .messageType(MessageType.SYSTEM)
                .sender(request.getUser())
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();
        chatMessageRepository.save(welcomeMessage);

        // Notify participants
        notificationManager.notifyChatRoomCreated(
                request.getAd().getUser().getId(),
                request.getExpert().getId(),
                request.getAd().getTitle(),
                savedChatRoom.getId());
        log.info("Chat room created successfully. ChatRoomId: {}", savedChatRoom.getId());

        return savedChatRoom;
    }

    public Optional<ChatRoom> getChatRoomByRequestId(String requestId) {
        return chatRoomRepository.findByRequestId(requestId);
    }

    @Transactional
    public void markMessagesAsRead(String chatRoomId, String userEmail) {
        ChatRoom chatRoom = getChatRoomWithValidation(chatRoomId, userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByChatRoomAndSenderIdAndIsReadFalse(chatRoom, user.getId());

        unreadMessages.forEach(message -> message.setRead(true));
        chatMessageRepository.saveAll(unreadMessages);
    }

    public long getUnreadMessageCount(String userEmail, String chatRoomId) {
        ChatRoom chatRoom = getChatRoomWithValidation(chatRoomId, userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return chatMessageRepository.countByChatRoomAndSenderIdAndIsReadFalse(chatRoom, user.getId());
    }

    public List<ChatRoom> getUserChatRooms(String userId) {
        return chatRoomRepository.findByUserIdOrderByLastActivityDesc(userId);
    }

    public List<ChatRoom> getExpertChatRooms(String expertId) {
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

        if (expert.isAccountBlocked()) {
            return Collections.emptyList();
        }

        return chatRoomRepository.findByExpertIdOrderByLastActivityDesc(expertId);
    }

    private boolean canAccessChatRoom(User user, ChatRoom chatRoom) {
        if (user instanceof Expert) {
            return chatRoom.getExpert().getId().equals(user.getId()) && !chatRoom.isExpertBlocked();
        }
        return chatRoom.getUser().getId().equals(user.getId());
    }

    public boolean canShareContact(String chatRoomId, String userId, String expertId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        // İletişim bilgisi paylaşımını kontrol et
        if (chatRoomRepository.hasContactBeenShared(chatRoomId)) {
            return false;
        }

        // Mesaj tipinden kontrol et
        if (chatMessageRepository.existsByChatRoomIdAndMessageType(chatRoomId, MessageType.CONTACT_INFO)) {
            return false;
        }

        // ContactSharingRecord'dan kontrol et
        return !contactSharingRecordRepository.existsByUserIdAndExpertIdAndAdId(
                userId, expertId, chatRoom.getAd().getId());
    }

    // Add this method to ensure messages can't be sent in completed chat rooms
    @Transactional
    public void validateMessageSending(ChatRoom chatRoom, User sender) {
        if (chatRoom.getStatus() == ChatRoomStatus.COMPLETED) {
            throw new BusinessException("Cannot send messages in a completed chat room");
        }

        if (!chatRoom.isActive()) {
            throw new BusinessException("Cannot send messages in an inactive chat room");
        }

        if (sender instanceof Expert && chatRoom.isExpertBlocked()) {
            throw new BusinessException("Expert is blocked and cannot send messages");
        }
    }

    private MessageType determineMessageType(String content, boolean containsContactInfo, String senderId) {
        if (containsContactInfo) {
            return MessageType.CONTACT_INFO;
        } else if ("SYSTEM".equals(senderId)) {
            return MessageType.SYSTEM;
        } else if (content.contains("PAYMENT_REQUEST")) {
            return MessageType.PAYMENT_REQUEST;
        }
        return MessageType.CHAT;
    }

    private String determineRecipientId(User sender, ChatRoom chatRoom) {
        return sender.getId().equals(chatRoom.getUser().getId())
                ? chatRoom.getExpert().getId()
                : chatRoom.getUser().getId();
    }

    @Transactional
    public void shareContactInformation(String chatRoomId, String userId) throws StripeException {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user instanceof Expert) {
            throw new BusinessException("Only users can share contact information");
        }

        Expert expert = chatRoom.getExpert();
        if (expert == null) {
            throw new BusinessException("No expert assigned to this chat room");
        }

        if (expert.isAccountBlocked()) {
            throw new BusinessException("Expert account is blocked");
        }

        if (chatRoom.isCompleted()) {
            throw new BusinessException("Cannot share contact information in a completed chat room");
        }

        if (!canShareContact(chatRoomId, userId, expert.getId())) {
            throw new BusinessException("Contact information has already been shared for this ad");
        }

        // Create contact sharing record
        ContactSharingRecord record = ContactSharingRecord.builder()
                .user(user)
                .expert(expert)
                .ad(chatRoom.getAd())
                .chatRoom(chatRoom)
                .sharedAt(LocalDateTime.now())
                .paymentProcessed(true)
                .build();
        contactSharingRecordRepository.save(record);

        // Create system message
        ChatMessage shareMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .content("Contact information has been shared")
                .messageType(MessageType.SYSTEM)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .contactInfo(true)
                .sender(user)
                .build();
        chatMessageRepository.save(shareMessage);

        // Notify expert
        notificationManager.notifyContactShared(expert.getId(), user.getId(), chatRoom.getId());
    }

}