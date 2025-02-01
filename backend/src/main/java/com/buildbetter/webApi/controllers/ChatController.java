package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.UserService;
import com.renovatipoint.business.concretes.ChatManager;
import com.renovatipoint.business.concretes.ChatValidationManager;
import com.renovatipoint.business.requests.CreateChatMessageRequest;
import com.renovatipoint.business.responses.ErrorResponse;
import com.renovatipoint.business.responses.GetChatMessageResponse;
import com.renovatipoint.business.responses.GetChatRoomResponse;
import com.renovatipoint.business.responses.GetUnreadCountResponse;
import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.business.responses.SuccessResponse;
import com.renovatipoint.entities.concretes.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final ChatManager chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final ChatValidationManager chatValidationManager;

    @MessageMapping("/chat")
    @SendToUser("/queue/messages")
    public GetChatMessageResponse handleMessage(
            CreateChatMessageRequest messageRequest,
            Authentication authentication) {
        try {
            // Get user from authentication
            String userEmail = authentication.getName();
            User user = userService.getByEmail(userEmail);

            log.debug("Received message from user {}: {}", userEmail, messageRequest);

            // Verify the sender is authorized
            validateSender(messageRequest, user);
            chatValidationManager.validateChatMessageRequest(messageRequest);

            ChatMessage message = chatService.sendMessage(
                    messageRequest.getSenderId(),
                    messageRequest.getChatRoomId(),
                    messageRequest.getContent(),
                    messageRequest.isContactInfo());

            GetChatMessageResponse response = GetChatMessageResponse.fromEntity(message);

            // Send to recipient
            notifyRecipient(message, messageRequest.getSenderId(), response);

            return response;
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            throw new BusinessException("Failed to process message: " + e.getMessage());
        }
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorResponse handleException(Exception exception) {
        log.error("WebSocket error:", exception);
        return new ErrorResponse(exception.getMessage());
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<GetChatRoomResponse>> getChatRooms(Principal principal) {
        try {
            User user = userService.getByEmail(principal.getName());
            List<ChatRoom> chatRooms;

            if (user instanceof Expert) {
                chatRooms = chatService.getExpertChatRooms(user.getId());
            } else {
                chatRooms = chatService.getUserChatRooms(user.getId());
            }

            List<GetChatRoomResponse> response = chatRooms.stream()
                    .map(GetChatRoomResponse::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching chat rooms", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/rooms/{chatRoomId}")
    public ResponseEntity<GetChatRoomResponse> getChatRoom(
            @PathVariable String chatRoomId,
            Principal principal) {
        try {
            ChatRoom chatRoom = chatService.getChatRoomWithValidation(chatRoomId, principal.getName());
            return ResponseEntity.ok(GetChatRoomResponse.fromEntity(chatRoom));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<Page<GetChatMessageResponse>> getChatMessages(
            @PathVariable String chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        try {
            Page<ChatMessage> messages = chatService.getChatMessages(
                    chatRoomId,
                    principal.getName(),
                    PageRequest.of(page, size, Sort.by("timestamp").descending()));

            Page<GetChatMessageResponse> response = messages.map(GetChatMessageResponse::fromEntity);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error fetching chat messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/rooms/{chatRoomId}/messages/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable String chatRoomId,
            Principal principal) {
        try {
            chatService.markMessagesAsRead(chatRoomId, principal.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/rooms/{chatRoomId}/unread")
    public ResponseEntity<GetUnreadCountResponse> getUnreadCount(
            @PathVariable String chatRoomId,
            Principal principal) {
        try {
            long count = chatService.getUnreadMessageCount(principal.getName(), chatRoomId);
            return ResponseEntity.ok(new GetUnreadCountResponse(count));
        } catch (Exception e) {
            log.error("Error getting unread count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/rooms/{chatRoomId}/complete")
    public ResponseEntity<?> markJobAsComplete(
            @PathVariable String chatRoomId,
            Principal principal) {
        try {
            User user = userService.getByEmail(principal.getName());
            if (!(user instanceof Expert)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Only experts can complete jobs"));
            }

            chatService.markJobAsComplete(chatRoomId, user.getId());
            return ResponseEntity.ok().body(new SuccessResponse("Job completed successfully"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error completing job", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to complete job: " + e.getMessage()));
        }
    }

    @PostMapping("/rooms/{chatRoomId}/share-contact")
    public ResponseEntity<?> shareContactInformation(
            @PathVariable String chatRoomId,
            Principal principal) {
        try {
            User user = userService.getByEmail(principal.getName());
            if (user instanceof Expert) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Only users can share contact information"));
            }

            chatService.shareContactInformation(chatRoomId, user.getId());
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error sharing contact information", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to share contact information: " + e.getMessage()));
        }
    }
    private void validateSender(CreateChatMessageRequest messageRequest, User user) {
        if (!"SYSTEM".equals(messageRequest.getSenderId()) &&
                !messageRequest.getSenderId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized message sender");
        }
    }

    private void notifyRecipient(ChatMessage message, String senderId, GetChatMessageResponse response) {
        String recipientId = message.getChatRoom().getUser().getId().equals(senderId)
                ? message.getChatRoom().getExpert().getId()
                : message.getChatRoom().getUser().getId();

        messagingTemplate.convertAndSendToUser(
                recipientId,
                "/queue/messages",
                response);
    }
}