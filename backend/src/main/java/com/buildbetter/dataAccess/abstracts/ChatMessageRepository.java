package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.ChatMessage;
import com.renovatipoint.entities.concretes.ChatRoom;
import com.renovatipoint.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    Page<ChatMessage> findByChatRoomId(String chatRoomId, Pageable pageable);

    List<ChatMessage> findByChatRoomAndSenderIdAndIsReadFalse(ChatRoom chatRoom, String id);

    long countByChatRoomAndSenderIdAndIsReadFalse(ChatRoom chatRoom, String id);

    @Query("SELECT COUNT(m) > 0 FROM ChatMessage m WHERE m.chatRoom.id = :chatRoomId AND m.messageType = :messageType")
    boolean existsByChatRoomIdAndMessageType(String chatRoomId, MessageType messageType);

    List<ChatMessage> findByChatRoomIdAndMessageTypeOrderByTimestampDesc(String chatRoomId, MessageType messageType);

    Optional<ChatMessage> findFirstByChatRoomIdAndMessageTypeOrderByTimestampDesc(String chatRoomId,
            MessageType messageType);
}
