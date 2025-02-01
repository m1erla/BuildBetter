package com.renovatipoint.business.abstracts;

import com.renovatipoint.entities.concretes.*;
import com.stripe.exception.StripeException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {
    ChatRoom getChatRoomWithValidation(String chatRoomId, String userEmail);

    Page<ChatMessage> getChatMessages(String chatRoomId, String userEmail, Pageable pageable);

    ChatMessage sendMessage(String senderId, String chatRoomId, String content, boolean isContactInfo)
            throws StripeException;

    void handleContactInformationSharing(Expert expert, User user, ChatRoom chatRoom,
            boolean isPendingPayment) throws StripeException;

    void markJobAsComplete(String chatRoomId, String expertId) throws StripeException;

    List<ChatRoom> getUserChatRooms(String userEmail);

    ChatRoom createChatRoomForAcceptedRequest(Request requestl);
}