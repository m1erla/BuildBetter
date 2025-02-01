package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(String recipientId);
    List<Notification> findByRecipientIdAndIsReadFalse(String recipientId);
}
