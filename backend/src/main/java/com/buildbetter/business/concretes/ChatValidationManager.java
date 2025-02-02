package com.buildbetter.business.concretes;

import com.buildbetter.business.requests.CreateChatMessageRequest;
import com.buildbetter.core.utilities.detector.ContactInfoDetector;
import com.buildbetter.core.utilities.exceptions.BusinessException;
import com.buildbetter.dataAccess.abstracts.ChatRoomRepository;
import com.buildbetter.dataAccess.abstracts.ContactSharingRecordRepository;
import com.buildbetter.dataAccess.abstracts.ExpertRepository;
import com.buildbetter.entities.concretes.ChatRoom;
import com.buildbetter.entities.concretes.Expert;
import com.buildbetter.entities.concretes.User;
import com.buildbetter.enums.ChatRoomStatus;
import com.buildbetter.enums.MessageType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatValidationManager {
    private final ChatRoomRepository chatRoomRepository;
    private final ExpertRepository expertRepository;
    private final ContactSharingRecordRepository contactSharingRecordRepository;

    public void validateChatMessageRequest(CreateChatMessageRequest request) {
        validateBasicMessageRequirements(request);
        ChatRoom chatRoom = validateChatRoomAccess(request);
        validateMessageContent(request, chatRoom);
    }

    public void validateContactSharing(ChatRoom chatRoom, User user, Expert expert) {
        validateChatRoomStatus(chatRoom);
        validateExpertStatus(expert);
        validateContactNotPreviouslyShared(chatRoom, user, expert);
    }

    public void validateJobCompletion(ChatRoom chatRoom, Expert expert) {
        validateChatRoomStatus(chatRoom);
        validateExpertStatus(expert);
        validateExpertPaymentSetup(expert);
        validateJobNotAlreadyCompleted(chatRoom);
    }

    private void validateBasicMessageRequirements(CreateChatMessageRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (request.getChatRoomId() == null) {
            throw new IllegalArgumentException("Chat room ID is required");
        }
        if (request.getSenderId() == null) {
            throw new IllegalArgumentException("Sender ID is required");
        }
    }

    private ChatRoom validateChatRoomAccess(CreateChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        if (chatRoom.getStatus() == ChatRoomStatus.COMPLETED || chatRoom.isCompleted()) {
            throw new BusinessException("Cannot send messages in a completed chat room");
        }

        if (!chatRoom.isActive()) {
            throw new IllegalStateException("Chat room is no longer active");
        }

        boolean hasAccess = chatRoom.getUser().getId().equals(request.getSenderId()) ||
                chatRoom.getExpert().getId().equals(request.getSenderId());
        if (!hasAccess) {
            throw new AccessDeniedException("Sender does not have access to this chat room");
        }

        if (chatRoom.getExpert().getId().equals(request.getSenderId()) && chatRoom.isExpertBlocked()) {
            throw new IllegalStateException("Expert account is blocked");
        }

        return chatRoom;
    }

    private void validateMessageContent(CreateChatMessageRequest request, ChatRoom chatRoom) {
        if (request.getType() == null) {
            request.setType(MessageType.CHAT);
        }

        if (request.getTimestamp() == null) {
            request.setTimestamp(LocalDateTime.now());
        }

        // Handle contact information detection
        if (!chatRoom.getExpert().getId().equals(request.getSenderId()) &&
                ContactInfoDetector.containsContactInformation(request.getContent())) {
            validateContactSharing(chatRoom,
                    chatRoom.getUser(),
                    chatRoom.getExpert());
            request.setType(MessageType.CONTACT_INFO);
            request.setContactInfo(true);
        }
    }

    private void validateChatRoomStatus(ChatRoom chatRoom) {
        if (chatRoom.getStatus() != ChatRoomStatus.ACTIVE) {
            throw new BusinessException("Chat room is not active");
        }
    }

    private void validateExpertStatus(Expert expert) {
        if (expert.isAccountBlocked()) {
            throw new IllegalStateException("Expert account is blocked");
        }
    }

    private void validateContactNotPreviouslyShared(ChatRoom chatRoom, User user, Expert expert) {
        if (chatRoom.isContactShared()) {
            throw new BusinessException("Contact information has already been shared in this chat room");
        }

        boolean contactExists = contactSharingRecordRepository
                .existsByUserIdAndExpertIdAndAdId(user.getId(), expert.getId(), chatRoom.getAd().getId());
        if (contactExists) {
            throw new BusinessException("Contact information has already been shared for this ad");
        }
    }

    private void validateExpertPaymentSetup(Expert expert) {
        if (expert.getStripeCustomerId() == null || expert.getPaymentInfo() == null) {
            throw new BusinessException("Expert has not set up payment information");
        }
    }

    private void validateJobNotAlreadyCompleted(ChatRoom chatRoom) {
        if (chatRoom.isCompleted() || chatRoom.getStatus() == ChatRoomStatus.COMPLETED) {
            throw new BusinessException("This job has already been completed");
        }
    }
}