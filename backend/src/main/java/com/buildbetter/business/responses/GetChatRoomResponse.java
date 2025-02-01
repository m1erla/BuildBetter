package com.renovatipoint.business.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.renovatipoint.entities.concretes.ChatMessage;
import com.renovatipoint.entities.concretes.ChatRoom;
import com.renovatipoint.entities.concretes.JobTitle;
import com.renovatipoint.enums.ChatRoomStatus;
import com.renovatipoint.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetChatRoomResponse {
    private String id;
    private String chatId;
    private UserInfo user;
    private ExpertInfo expert;
    private AdInfo ad;
    private boolean active;
    private boolean expertBlocked;
    private String lastActivity;
    private ChatRoomStatus status;
    private List<GetChatMessageResponse> recentMessages;

    // Contact sharing information
    private boolean contactShared;
    private String contactSharedAt;

    // Completion information
    private boolean completed;
    private boolean completionPaymentProcessed;
    private String completedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String name;
        private String email;
        private String profileImage;  // Added for UI display
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertInfo {
        private String id;
        private String name;
        private String email;
        private String companyName;
        private boolean accountBlocked;
        private String profileImage;  // Added for UI display
        private String jobTitle;      // Added for better expert information
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdInfo {
        private String id;
        private String title;
        private String descriptions;
        private String imageUrl;      // Added for UI display
        private CategoryInfo category; // Added for better context
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private String id;
        private String name;
    }

    // Enhanced conversion method with better null handling and date formatting
    public static GetChatRoomResponse fromEntity(ChatRoom chatRoom) {
        return GetChatRoomResponse.builder()
                .id(chatRoom.getId())
                .chatId(chatRoom.getChatId())
                .user(UserInfo.builder()
                        .id(chatRoom.getUser().getId())
                        .name(chatRoom.getUser().getName())
                        .email(chatRoom.getUser().getEmail())
                        .profileImage(chatRoom.getUser().getProfileImage())
                        .build())
                .expert(ExpertInfo.builder()
                        .id(chatRoom.getExpert().getId())
                        .name(chatRoom.getExpert().getName())
                        .email(chatRoom.getExpert().getEmail())
                        .companyName(chatRoom.getExpert().getCompanyName())
                        .accountBlocked(chatRoom.getExpert().isAccountBlocked())
                        .profileImage(chatRoom.getExpert().getProfileImage())
                        .jobTitle(Optional.ofNullable(chatRoom.getExpert().getJobTitle())
                                .map(JobTitle::getName)
                                .orElse(null))
                        .build())
                .ad(AdInfo.builder()
                        .id(chatRoom.getAd().getId())
                        .title(chatRoom.getAd().getTitle())
                        .descriptions(chatRoom.getAd().getDescriptions())
                        .imageUrl(chatRoom.getAd().getImageUrl())
                        .category(chatRoom.getAd().getCategory() != null ?
                                CategoryInfo.builder()
                                        .id(chatRoom.getAd().getCategory().getId())
                                        .name(chatRoom.getAd().getCategory().getName())
                                        .build() : null)
                        .build())
                .active(chatRoom.isActive())
                .expertBlocked(chatRoom.isExpertBlocked())
                .status(chatRoom.getStatus())
                .lastActivity(formatDateTime(chatRoom.getLastActivity()))
                .contactShared(chatRoom.isContactShared())
                .contactSharedAt(formatDateTime(chatRoom.getContactSharedAt()))
                .completed(chatRoom.isCompleted())
                .completionPaymentProcessed(chatRoom.isCompletionPaymentProcessed())
                .completedAt(formatDateTime(chatRoom.getCompletedAt()))
                .recentMessages(getRecentMessages(chatRoom))
                .build();
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ?
                dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    private static List<GetChatMessageResponse> getRecentMessages(ChatRoom chatRoom) {
        return chatRoom.getMessages().stream()
                .sorted(Comparator.comparing(ChatMessage::getTimestamp).reversed())
                .limit(10)
                .map(GetChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }
}

