package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.ContactSharingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactSharingRecordRepository extends JpaRepository<ContactSharingRecord, String> {
    boolean existsByUserIdAndExpertIdAndAdId(String userId, String expertId, String adId);

    List<ContactSharingRecord> findByChatRoomId(String chatRoomId);

    @Query("SELECT COUNT(c) > 0 FROM ContactSharingRecord c WHERE c.chatRoom.id = :chatRoomId")
    boolean existsByChatRoomId(String chatRoomId);

    Optional<ContactSharingRecord> findFirstByChatRoomIdOrderBySharedAtDesc(String chatRoomId);

    @Query("SELECT c FROM ContactSharingRecord c WHERE c.chatRoom.id = :chatRoomId AND c.paymentProcessed = true")
    List<ContactSharingRecord> findPaidRecordsByChatRoomId(String chatRoomId);
}