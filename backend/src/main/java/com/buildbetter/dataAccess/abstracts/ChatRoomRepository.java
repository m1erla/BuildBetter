package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.ChatRoom;
import com.renovatipoint.entities.concretes.Request;
import com.renovatipoint.enums.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    Optional<ChatRoom> findByRequestId(String requestId);

    Optional<ChatRoom> findByRequest(Request request);

    Optional<ChatRoom> findByUserId(String userId);

    List<ChatRoom> findByExpertId(String expertId);

    List<ChatRoom> findByUserIdOrderByLastActivityDesc(String userId);

    List<ChatRoom> findByExpertIdOrderByLastActivityDesc(String expertId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.expert.id = :expertId AND cr.status = :status")
    List<ChatRoom> findByExpertIdAndStatus(String expertId, ChatRoomStatus status);

    @Query("SELECT COUNT(cr) > 0 FROM ChatRoom cr JOIN cr.contactSharingRecords csr WHERE cr.id = :chatRoomId")
    boolean hasContactBeenShared(String chatRoomId);

    @Query("SELECT COUNT(cr) > 0 FROM ChatRoom cr WHERE cr.id = :chatRoomId AND (cr.completed = true OR cr.status = 'COMPLETED' OR cr.completionPaymentProcessed = true)")
    boolean isCompleted(String chatRoomId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.expert.id = :expertId AND cr.completed = true AND cr.completionPaymentProcessed = true")
    List<ChatRoom> findCompletedRoomsByExpertId(String expertId);
}
