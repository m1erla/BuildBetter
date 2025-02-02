package com.buildbetter.core.utilities.listeners;

import com.buildbetter.business.concretes.ChatManager;
import com.buildbetter.business.concretes.NotificationManager;
import com.buildbetter.core.utilities.events.RequestStatusChangedEvent;
import com.buildbetter.entities.concretes.ChatRoom;
import com.buildbetter.entities.concretes.Request;
import com.buildbetter.enums.RequestStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RequestStatusChangeListener {
    private final ChatManager chatService;
    private final NotificationManager notificationService;
    private final static Logger logger = LoggerFactory.getLogger(RequestStatusChangeListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestStatusChange(RequestStatusChangedEvent event) {
        Request request = event.getRequest();

        if (request.getStatus() == RequestStatus.ACCEPTED) {
            try {
                ChatRoom chatRoom = chatService.createChatRoomForAcceptedRequest(request);

                // Notify both parties
                notificationService.notifyRequestAccepted(
                        request.getExpert().getId(),
                        request.getAd().getTitle(),
                        chatRoom.getId()
                );
            } catch (Exception e) {
                logger.error("Failed to create chat room for request: " + request.getId(), e);
            }
        }
    }
}