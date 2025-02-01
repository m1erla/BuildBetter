package com.renovatipoint.business.concretes;

import com.renovatipoint.business.responses.GetRequestsResponse;
import com.renovatipoint.core.utilities.events.RequestStatusChangedEvent;
import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.core.utilities.mappers.ModelMapperManager;
import com.renovatipoint.dataAccess.abstracts.AdsRepository;
import com.renovatipoint.dataAccess.abstracts.ExpertRepository;
import com.renovatipoint.dataAccess.abstracts.RequestRepository;
import com.renovatipoint.entities.concretes.*;
import com.renovatipoint.enums.RequestStatus;
import com.stripe.exception.StripeException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RequestManager {
    private final static Logger logger = LoggerFactory.getLogger(RequestManager.class);
    private final RequestRepository requestRepository;
    private final ChatManager chatRoomManager;
    private final ExpertRepository expertRepository;
    private final NotificationManager notificationManager;
    private final AdsRepository adsRepository;
    private final ModelMapperManager modelMapperManager;
    private final ApplicationEventPublisher eventPublisher;
//    private final OperationBusinessManager operationBusinessManager;



@Transactional
public Request createRequest(String adId, String expertEmail) {
    logger.info("Creating request for expert email {} and ad {}", expertEmail, adId);

    // Retrieve the Expert directly from the expertRepository by email
    Expert expert = expertRepository.findByEmail(expertEmail)
            .orElseThrow(() -> {
                logger.error("Expert not found with email: {}", expertEmail);
                return new EntityNotFoundException("Expert not found with email: " + expertEmail);
            });

    // Ensure the Expert has a valid ID
    if (expert.getId() == null) {
        logger.error("Expert ID is null or invalid for email: {}", expertEmail);
        throw new IllegalStateException("Expert ID is invalid. Please verify expert registration.");
    }

    // Retrieve the ad from the adsRepository
    Ads ad = adsRepository.findById(adId)
            .orElseThrow(() -> {
                logger.error("Ad not found with id: {}", adId);
                return new EntityNotFoundException("Ad not found with id: " + adId);
            });

    logger.info("Found ad: {}", ad.getTitle());


    Request request = Request.builder()
            .expert(expert)
            .user(ad.getUser())  // Assign the expert as the user (since Expert is a subclass of User)
            .ad(ad)
            .status(RequestStatus.PENDING)
            .build();

    Request savedRequest = requestRepository.save(request);

    notificationManager.notifyRequestCreated(request.getExpert().getId(), request.getMessage(), ad.getTitle());
    logger.info("Request created successfully with id: {}", savedRequest.getId());
    return savedRequest;
}
    @Transactional
    public Request acceptRequest(String requestId, String userId) throws StripeException {
        // Retrieve the request by its ID
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));

        // Check if the user accepting the request is the owner of the ad
        if (!request.getAd().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the ad owner can accept this request");
        }

        // Ensure the request is in the correct state
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("This request cannot be accepted in its current state");
        }

        // Retrieve the expert associated with the request
        Expert expert = request.getExpert();
        if (expert == null) {
            throw new EntityNotFoundException("Expert not found");
        }

        // Change the status of the request to ACCEPTED
        request.setStatus(RequestStatus.ACCEPTED);
        Request savedRequest = requestRepository.save(request);

        ChatRoom chatRoom = chatRoomManager.createChatRoomForAcceptedRequest(savedRequest);

        notificationManager.notifyRequestAccepted(request.getExpert().getId(), request.getAd().getTitle(), chatRoom.getChatId());
        eventPublisher.publishEvent(new RequestStatusChangedEvent(this, savedRequest));


        return savedRequest;
    }


    public List<GetRequestsResponse> getRequestsByAdOwner(String userId) {
        List<Request> requests = requestRepository.findRequestsByAdOwner(userId);
        return requests.stream()
                .map(request -> this.modelMapperManager.forResponse().map(request, GetRequestsResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public Request completeRequest(String requestId, String expertId) throws StripeException {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));

        request.setStatus(RequestStatus.COMPLETED);
        Request savedRequest = requestRepository.save(request);

        // Charge the expert for job completion
//        operationBusinessManager.createJobCompletionTransaction(expertId, request.getAd().getUser().getId(), requestId);

        return savedRequest;
    }
    @Transactional
    public Request rejectRequest(String requestId, String userId){
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));

        if (!request.getAd().getUser().getId().equals(userId)){
            throw new IllegalArgumentException("Only the ad owner can reject this request");
        }
        if (request.getStatus() != RequestStatus.PENDING){
            throw new IllegalStateException("This request cannot be rejected in its current state");
        }
        request.setStatus(RequestStatus.REJECTED);
        return requestRepository.save(request);
    }

    public List<GetRequestsResponse> getRequestsByUser(String userId) {
        List<Request> requests = requestRepository.findByUserId(userId);
        return requests.stream()
                .map(request -> this.modelMapperManager.forResponse().map(request, GetRequestsResponse.class))
                .collect(Collectors.toList());
    }
    private GetRequestsResponse mapToGetRequestsResponse(Request request) {
        GetRequestsResponse response = new GetRequestsResponse();
        response.setId(request.getId());
        response.setExpertName(request.getExpert() != null ? request.getExpert().getName() : "N/A");
        response.setUserName(request.getUser() != null ? request.getUser().getName() : "N/A");
        response.setAdTitle(request.getAd() != null ? request.getAd().getTitle() : "N/A");
        response.setStatus(request.getStatus());
        response.setMessage(request.getMessage() != null ? request.getMessage() : "No message");
        return response;
    }

    public List<GetRequestsResponse> getRequestsByExpert(String expertId) {
        List<Request> requests = requestRepository.findByExpertId(expertId);
        return requests.stream()
                .map(request -> this.modelMapperManager.forResponse().map(request, GetRequestsResponse.class))
                .collect(Collectors.toList());
    }

    public List<Request> getRequestsByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    @Transactional
    public Request cancelRequest(String requestId, String userId){
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));

        if (!(request.getAd().getUser().getId() == userId)){
            throw new BusinessException("Only the user who created the request can cancel it");
        }
        if (request.getStatus() != RequestStatus.PENDING){
            throw new BusinessException("Only pending request can be cancelled");
        }
        request.setStatus(RequestStatus.CANCELLED);
        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<Request> getPendingRequestsForAd(String adId) {
        Ads ad = adsRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("Ad not found"));

        return requestRepository.findByAdAndStatus(ad, RequestStatus.PENDING);
    }

}
// Build the response object with all necessary data
//        GetRequestsResponse response = new GetRequestsResponse();
//        response.setId(request.getId());
//        response.setAdName(request.getAd().getName());
//        response.setExpertName(expert.getName());
//        response.setExpertEmail(expert.getEmail());
//        response.setExpertId(expert.getId());
//        response.setUserName(request.getUser().getName());
//        response.setStatus(request.getStatus().name());
//        response.setMessage(request.getMessage());
    //    @Transactional
//    public Request createRequest(String expertId, String adId, CreateRequestDTO requestDTO) {
//        if (expertId == null || adId == null) {
//            throw new IllegalArgumentException("userId, expertId, and adId must not be null");
//        }
//
//        Expert expert = expertRepository.findById(expertId)
//                .orElseThrow(() -> new EntityNotFoundException("Expert not found with id: " + expertId));
//
//        Ads ad = adsRepository.findById(adId)
//                .orElseThrow(() -> new EntityNotFoundException("Ad not found with id: " + adId));
//
//        Request request = Request.builder()
//                .expert(expert)
//                .ad(ad)
//                .status(RequestStatus.PENDING)
//                .message(requestDTO.getMessage())
//                .urgentRequest(requestDTO.isUrgentRequest())
//                .proposedPrice(requestDTO.getProposePrice())
//                .proposedTimeLine(requestDTO.getProposedTimeLine())
//                .build();
//
//        return requestRepository.save(request);
//    }
//    @Transactional
//    public Request acceptRequest(String requestId, String userId) {
//        Request request = requestRepository.findById(requestId)
//                .orElseThrow(() -> new EntityNotFoundException("Request not found"));
//
//        if (!request.getUser().getId().equals(userId)) {
//            throw new IllegalArgumentException("This request does not belong to the specified user");
//        }
//
//        request.setStatus(RequestStatus.ACCEPTED);
//        return requestRepository.save(request);
//    }

//    @Transactional
//    public Request rejectRequest(String requestId, String userId) {
//        Request request = requestRepository.findById(requestId)
//                .orElseThrow(() -> new EntityNotFoundException("Request not found"));
//
//        if (!request.getUser().getId().equals(userId)) {
//            throw new IllegalArgumentException("This request does not belong to the specified user");
//        }
//
//        request.setStatus(RequestStatus.REJECTED);
//        return requestRepository.save(request);
//    }



