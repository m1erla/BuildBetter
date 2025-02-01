//package com.renovatipoint.dataAccess.abstracts;
//
//import com.renovatipoint.entities.concretes.Operation;
//import com.renovatipoint.enums.OperationStatus;
//import com.renovatipoint.enums.OperationType;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//
//public interface OperationRepository extends JpaRepository<Operation, String> {
//    List<Operation> findByUserId(String userId);
//    List<Operation> findByExpertId(String expertId);
//    List<Operation> findByType(OperationType type);
//    List<Operation> findByStatus(OperationStatus status);
////    List<Operation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
////    List<Operation> findByExpertIdAndCreatedAtBetween(String expertId, LocalDateTime start, LocalDateTime end);
//}
