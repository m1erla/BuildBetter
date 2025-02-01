package com.renovatipoint.business.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.renovatipoint.enums.MessageType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateChatMessageRequest {
    @NotBlank(message = "Sender ID is required")
    private String senderId;

    @NotBlank(message = "Chat room ID is required")
    private String chatRoomId;

    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;

    private boolean isContactInfo;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;

    // Method renamed for clarity and consistency
    public void setContactInfo(boolean isContact) {
        this.isContactInfo = isContact;
    }

    // Additional validation method
    public void validate() {
        if (content != null && content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        if (type == null) {
            type = MessageType.CHAT;
        }
    }
}