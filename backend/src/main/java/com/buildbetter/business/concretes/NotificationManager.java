package com.renovatipoint.business.concretes;

import com.renovatipoint.business.responses.GetNotificationResponse;
import com.renovatipoint.dataAccess.abstracts.ChatRoomRepository;
import com.renovatipoint.dataAccess.abstracts.NotificationRepository;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import com.renovatipoint.entities.concretes.ChatRoom;
import com.renovatipoint.entities.concretes.Notification;
import com.renovatipoint.entities.concretes.User;
import com.renovatipoint.enums.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationManager {
        private final NotificationRepository notificationRepository;
        private final UserRepository userRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final ChatRoomRepository chatRoomRepository;

        @Transactional
        public void notifyRequestCreated(String userId, String expertName, String adTitle) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException("User not found"));

                Notification notification = Notification.builder()
                                .recipient(user)
                                .title("New Request")
                                .message("New request from " + expertName + " for '" + adTitle + "'")
                                .type(NotificationType.NEW_REQUEST)
                                .createdAt(LocalDateTime.now())
                                .build();

                notificationRepository.save(notification);

                GetNotificationResponse notificationDTO = GetNotificationResponse.fromEntity(notification);

                messagingTemplate.convertAndSendToUser(
                                userId,
                                "/queue/notifications",
                                notificationDTO);
        }

        @Transactional
        public void notifyRequestAccepted(String expertId, String adTitle, String chatId) {
                User expert = userRepository.findById(expertId)
                                .orElseThrow(() -> new EntityNotFoundException("Expert not found"));

                // Create notification entity
                Notification notification = Notification.builder()
                                .recipient(expert)
                                .title("Request Accepted")
                                .message("Your request for '" + adTitle + "' has been accepted")
                                .type(NotificationType.REQUEST_ACCEPTED)
                                .relatedEntityId(chatId)
                                .createdAt(LocalDateTime.now())
                                .build();

                notificationRepository.save(notification);

                // Send real-time notification via WebSocket
                GetNotificationResponse notificationDTO = GetNotificationResponse.fromEntity(notification);

                messagingTemplate.convertAndSendToUser(
                                expertId,
                                "/queue/notifications",
                                notificationDTO);
        }

        @Transactional
        public void notifyNewMessage(String recipientId, String senderId, String chatRoomId, String content) {
                User recipient = userRepository.findById(recipientId)
                                .orElseThrow(() -> new EntityNotFoundException("Recipient not found"));
                User sender = userRepository.findById(senderId)
                                .orElseThrow(() -> new EntityNotFoundException("Sender not found"));
                ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

                // Create notification entity
                Notification notification = Notification.builder()
                                .recipient(recipient)
                                .title("New Message")
                                .message(String.format("New message from %s regarding ad: %s",
                                                sender.getName(),
                                                chatRoom.getAd().getTitle()))
                                .type(NotificationType.NEW_MESSAGE)
                                .relatedEntityId(chatRoomId)
                                .isRead(false)
                                .build();

                notificationRepository.save(notification);

                // Send real-time notification
                GetNotificationResponse notificationResponse = GetNotificationResponse.fromEntity(notification);
                messagingTemplate.convertAndSendToUser(
                                recipientId,
                                "/queue/notifications",
                                notificationResponse);
        }

        @Transactional
        public void notifyChatRoomCreated(String userId, String expertId, String adTitle, String chatRoomId) {
                // Notify user
                createAndSendNotification(
                                userId,
                                "Chat Room Created",
                                String.format("Chat room created for your ad: %s", adTitle),
                                NotificationType.CHAT_ROOM_CREATED,
                                chatRoomId);

                // Notify expert
                createAndSendNotification(
                                expertId,
                                "Chat Room Created",
                                String.format("Chat room created for ad: %s", adTitle),
                                NotificationType.CHAT_ROOM_CREATED,
                                chatRoomId);
        }

        @Transactional
        public void notifyChatRoomCompleted(String userId, String expertId, String adTitle) {
                // Notify user
                createAndSendNotification(
                                userId,
                                "Chat Room Completed",
                                String.format("Chat room for ad '%s' has been marked as completed", adTitle),
                                NotificationType.CHAT_ROOM_COMPLETED,
                                null);

                // Notify expert
                createAndSendNotification(
                                expertId,
                                "Chat Room Completed",
                                String.format("Chat room for ad '%s' has been marked as completed", adTitle),
                                NotificationType.CHAT_ROOM_COMPLETED,
                                null);
        }

        @Transactional
        public void notifyContactInformationShared(String userId, String expertId, String adTitle, String chatRoomId) {
                // Notify user
                createAndSendNotification(
                                userId,
                                "Contact Information Shared",
                                "You have shared contact information. The expert will be charged €1",
                                NotificationType.CONTACT_INFO_SHARED,
                                chatRoomId);

                // Notify expert
                createAndSendNotification(
                                expertId,
                                "Contact Information Received",
                                String.format("Contact information received for ad '%s'. Your account will be charged €1",
                                                adTitle),
                                NotificationType.CONTACT_INFO_SHARED,
                                chatRoomId);
        }

        @Transactional
        public void notifyExpertBlocked(String expertId, String reason) {
                createAndSendNotification(
                                expertId,
                                "Account Blocked",
                                String.format("Your account has been blocked. Reason: %s", reason),
                                NotificationType.ACCOUNT_BLOCKED,
                                null);
        }

        @Transactional
        public void notifyInsufficientBalance(String expertId, String adTitle) {
                createAndSendNotification(
                                expertId,
                                "Insufficient Balance",
                                String.format("Unable to receive contact information for ad '%s'. Please top up your balance.",
                                                adTitle),
                                NotificationType.INSUFFICIENT_BALANCE,
                                null);
        }

        public List<GetNotificationResponse> getUnreadNotifications(String userId) {
                List<Notification> unreadNotifications = notificationRepository
                                .findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId);

                return unreadNotifications.stream()
                                .map(this::convertToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void markAsRead(String notificationId, String userId) {
                Notification notification = notificationRepository.findById(notificationId)
                                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

                if (!notification.getRecipient().getId().equals(userId)) {
                        throw new AccessDeniedException("Cannot mark someone else's notification as read");
                }

                notification.setRead(true);
                notificationRepository.save(notification);
        }

        @Transactional
        public void markAllAsRead(String userId) {
                List<Notification> unreadNotifications = notificationRepository.findByRecipientIdAndIsReadFalse(userId);
                unreadNotifications.forEach(notification -> notification.setRead(true));
                notificationRepository.saveAll(unreadNotifications);
        }

        public void createAndSendNotification(
                        String recipientId,
                        String title,
                        String message,
                        NotificationType type,
                        String relatedEntityId) {

                User recipient = userRepository.findById(recipientId)
                                .orElseThrow(() -> new EntityNotFoundException("Recipient not found"));

                Notification notification = Notification.builder()
                                .recipient(recipient)
                                .title(title)
                                .message(message)
                                .type(type)
                                .relatedEntityId(relatedEntityId)
                                .isRead(false)
                                .build();

                notification = notificationRepository.save(notification);

                GetNotificationResponse response = convertToResponse(notification);

                messagingTemplate.convertAndSendToUser(
                                recipientId,
                                "/queue/notifications",
                                response);
        }

        private GetNotificationResponse convertToResponse(Notification notification) {
                return GetNotificationResponse.fromEntity(notification);
        }

        public void notifyJobCompleted(String userId, String expertId, String adTitle, String chatRoomId) {
                // Notify user
                createAndSendNotification(
                                userId,
                                "Job Completed",
                                "The expert has marked the job '" + adTitle
                                                + "' as completed. A completion fee of €5 has been processed.",
                                NotificationType.CHAT_ROOM_COMPLETED,
                                chatRoomId);

                // Notify expert
                createAndSendNotification(
                                expertId,
                                "Job Marked as Complete",
                                "You have marked the job '" + adTitle
                                                + "' as completed. The completion fee has been processed.",
                                NotificationType.CHAT_ROOM_COMPLETED,
                                chatRoomId);
        }

        public void notifyContactShared(String expertId, String userId, String chatRoomId) {
                ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

                notifyContactInformationShared(userId, expertId, chatRoom.getAd().getTitle(), chatRoomId);
        }
}