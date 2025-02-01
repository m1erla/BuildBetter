package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.ExpertService;
import com.renovatipoint.business.abstracts.UserService;
import com.renovatipoint.business.concretes.ChatManager;
import com.renovatipoint.business.concretes.NotificationManager;
import com.renovatipoint.business.concretes.RequestManager;
import com.renovatipoint.business.requests.CreateRequestDTO;
import com.renovatipoint.business.responses.GetRequestAcceptedResponse;
import com.renovatipoint.business.responses.GetRequestsResponse;
import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.entities.concretes.ChatRoom;
import com.renovatipoint.entities.concretes.Expert;
import com.renovatipoint.entities.concretes.Request;
import com.renovatipoint.entities.concretes.User;
import com.renovatipoint.enums.NotificationType;
import com.renovatipoint.enums.RequestStatus;
import com.renovatipoint.security.jwt.JwtService;
import com.stripe.exception.ApiException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestController {
    private final RequestManager requestManager;
    private final ChatManager chatService;
    private final NotificationManager notificationManager;
    private final UserService userService;
    private final ExpertService expertService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<GetRequestsResponse> createRequest(
            @RequestBody CreateRequestDTO requestDTO,
            @RequestHeader("Authorization") String token) {
        log.info("Received request to create request for ad: {}", requestDTO.getAdId());
        try {
            String userEmail = jwtService.extractUsername(token.replace("Bearer ", ""));
            if (userEmail == null) {
                throw new BusinessException("Invalid token");
            }

            Request createdRequest = requestManager.createRequest(requestDTO.getAdId(), userEmail);
            log.info("Request created successfully: {}", createdRequest.getId());

            // Notify ad owner about new request
            notificationManager.createAndSendNotification(
                    createdRequest.getAd().getUser().getId(),
                    "New Request",
                    "New request received for your ad: " + createdRequest.getAd().getTitle(),
                    NotificationType.NEW_REQUEST,
                    createdRequest.getId());
            GetRequestsResponse response = GetRequestsResponse.builder()
                    .id(createdRequest.getId())
                    .expertId(createdRequest.getExpert().getId())
                    .expertName(createdRequest.getExpert().getName())
                    .userId(createdRequest.getAd().getUser().getId())
                    .userName(createdRequest.getAd().getUser().getName())
                    .adId(createdRequest.getAd().getId())
                    .adTitle(createdRequest.getAd().getTitle())
                    .status(createdRequest.getStatus())
                    .createdAt(createdRequest.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .message(createdRequest.getMessage())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating request", e);
            throw new BusinessException("Error creating request: " + e.getMessage());
        }
    }

    @PutMapping("/{requestId}/accept")
    public ResponseEntity<GetRequestAcceptedResponse> acceptRequest(
            @PathVariable String requestId,
            @RequestHeader("Authorization") String token) {
        try {
            log.info("Accepting request with ID: {}", requestId);
            String userEmail = jwtService.extractUsername(token.replace("Bearer ", ""));
            User user = userService.getByEmail(userEmail);

            Request acceptedRequest = requestManager.acceptRequest(requestId, user.getId());
            ChatRoom chatRoom = chatService.createChatRoomForAcceptedRequest(acceptedRequest);

            GetRequestAcceptedResponse response = GetRequestAcceptedResponse.builder()
                    .requestId(acceptedRequest.getId())
                    .status(acceptedRequest.getStatus())
                    .chatRoomId(chatRoom.getId())
                    .expertId(acceptedRequest.getExpert().getId())
                    .userId(acceptedRequest.getUser().getId())
                    .adId(acceptedRequest.getAd().getId())
                    .adTitle(acceptedRequest.getAd().getTitle())
                    .build();

            log.info("Request accepted successfully. RequestId: {}, ChatRoomId: {}",
                    requestId, chatRoom.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error accepting request: {}", e.getMessage(), e);
            throw new BusinessException("Error accepting request: " + e.getMessage());
        }
    }

    @PutMapping("/{requestId}/reject")
    public ResponseEntity<GetRequestsResponse> rejectRequest(
            @PathVariable String requestId,
            @RequestParam String userId,
            Principal principal) {
        try {
            // Verify user
            if (!userService.getByEmail(principal.getName()).getId().equals(userId)) {
                throw new AccessDeniedException("Unauthorized access");
            }

            Request rejectedRequest = requestManager.rejectRequest(requestId, userId);

            // Notify expert about rejection
            notificationManager.createAndSendNotification(
                    rejectedRequest.getExpert().getId(),
                    "Request Rejected",
                    "Your request for ad: " + rejectedRequest.getAd().getTitle() + " has been rejected",
                    NotificationType.REQUEST_REJECTED,
                    rejectedRequest.getId());

            return ResponseEntity.ok(GetRequestsResponse.fromEntity(rejectedRequest));
        } catch (Exception e) {
            log.error("Error rejecting request", e);
            throw new BusinessException("Error rejecting request: " + e.getMessage());
        }
    }

    @PutMapping("/{requestId}/complete")
    public ResponseEntity<GetRequestsResponse> completeRequest(
            @PathVariable String requestId,
            @RequestParam String expertId,
            Principal principal) {
        try {
            // Verify expert
            Expert expert = expertService.getByEmail(principal.getName());
            if (!expert.getId().equals(expertId)) {
                throw new AccessDeniedException("Unauthorized access");
            }

            Request completedRequest = requestManager.completeRequest(requestId, expertId);

            // Complete the chat room
            ChatRoom chatRoom = chatService.getChatRoomByRequestId(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));
            chatService.markJobAsComplete(chatRoom.getId(), expertId);

            return ResponseEntity.ok(GetRequestsResponse.fromEntity(completedRequest));
        } catch (Exception e) {
            log.error("Error completing request", e);
            throw new BusinessException("Error completing request: " + e.getMessage());
        }
    }

    @GetMapping("/ads/owner/{userId}")
    public ResponseEntity<List<GetRequestsResponse>> getRequestsByAdOwner(
            @PathVariable String userId,
            Principal principal) {
        if (!userService.getByEmail(principal.getName()).getId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access");
        }
        return ResponseEntity.ok(requestManager.getRequestsByAdOwner(userId));
    }

    @GetMapping("/expert/{expertId}")
    public ResponseEntity<List<GetRequestsResponse>> getRequestsByExpert(
            @PathVariable String expertId,
            Principal principal) {
        if (!expertService.getByEmail(principal.getName()).getId().equals(expertId)) {
            throw new AccessDeniedException("Unauthorized access");
        }
        return ResponseEntity.ok(requestManager.getRequestsByExpert(expertId));
    }

    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<GetRequestsResponse> cancelRequest(
            @PathVariable String requestId,
            @RequestParam String userId,
            Principal principal) {
        try {
            if (!userService.getByEmail(principal.getName()).getId().equals(userId)) {
                throw new AccessDeniedException("Unauthorized access");
            }

            Request cancelledRequest = requestManager.cancelRequest(requestId, userId);

            // Notify participants
            notificationManager.createAndSendNotification(
                    cancelledRequest.getExpert().getId(),
                    "Request Cancelled",
                    "Request for ad: " + cancelledRequest.getAd().getTitle() + " has been cancelled",
                    NotificationType.REQUEST_CANCELLED,
                    cancelledRequest.getId());

            return ResponseEntity.ok(GetRequestsResponse.fromEntity(cancelledRequest));
        } catch (Exception e) {
            log.error("Error cancelling request", e);
            throw new BusinessException("Error cancelling request: " + e.getMessage());
        }
    }
}

// @PostMapping
// public ResponseEntity<?> createRequest(@Validated @RequestBody
// CreateRequestDTO requestDTO) {
// Authentication authentication =
// SecurityContextHolder.getContext().getAuthentication();
// String currentUserEmail = authentication.getName();
//
// User currentUser = userService.getByEmail(currentUserEmail);
// if (!(currentUser instanceof Expert)) {
// return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only experts can
// create requests");
// }
//
// try {
// Request createdRequest = requestManager.createRequest(currentUser.getId(),
// requestDTO.getAdId(), requestDTO);
// return ResponseEntity.ok(createdRequest);
// } catch (Exception e) {
// return ResponseEntity.badRequest().body("Failed to create request: " +
// e.getMessage());
// }
// }
// @PutMapping("/{requestId}/reject")
// public ResponseEntity<Request> rejectRequest(@PathVariable String requestId,
// @RequestParam String expertId) {
// Request request = requestManager.rejectRequest(requestId, expertId);
// return ResponseEntity.ok(request);
// }

// @PostMapping
// public ResponseEntity<?> createRequest(@RequestBody CreateRequestDTO
// requestDTO) {
// try {
// Request createdRequest = requestManager.createRequest(requestDTO.getAdId(),
// requestDTO.getExpertEmail());
// return ResponseEntity.ok(createdRequest);
// } catch (Exception e) {
// return ResponseEntity.badRequest().body("Error creating request: " +
// e.getMessage());
// }
// }

// @PutMapping("/{requestId}/accept")
// public ResponseEntity<?> acceptRequest(@PathVariable String requestId,
// @RequestParam String userId) {
// try {
// Request acceptedRequest = requestManager.acceptRequest(requestId, userId);
// return ResponseEntity.ok(acceptedRequest);
// } catch (Exception e) {
// return ResponseEntity.badRequest().body(e.getMessage());
// }
// }

// @PutMapping("/{requestId}/reject")
// public ResponseEntity<?> rejectRequest(@PathVariable String requestId,
// @RequestParam String userId) {
// try {
// Request rejectedRequest = requestManager.rejectRequest(requestId, userId);
// return ResponseEntity.ok(rejectedRequest);
// } catch (Exception e) {
// return ResponseEntity.badRequest().body(e.getMessage());
// }
// }
// @PutMapping("/{requestId}/accept")
// public ResponseEntity<?> acceptRequest(@PathVariable String requestId) {
// Authentication authentication =
// SecurityContextHolder.getContext().getAuthentication();
// String currentUserEmail = authentication.getName();
// User currentUser = userService.getByEmail(currentUserEmail);
//
// try {
// Request acceptedRequest = requestManager.acceptRequest(requestId,
// currentUser.getId());
// return ResponseEntity.ok(acceptedRequest);
// } catch (EntityNotFoundException | IllegalArgumentException |
// IllegalStateException e) {
// return ResponseEntity.badRequest().body(e.getMessage());
// } catch (StripeException e) {
// return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
// .body("An error occurred while processing the payment");
// }
// }
