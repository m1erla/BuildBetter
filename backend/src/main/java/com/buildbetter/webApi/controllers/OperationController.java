//package com.renovatipoint.webApi.controllers;
//
//import com.renovatipoint.business.concretes.OperationBusinessManager;
//import com.renovatipoint.core.utilities.exceptions.BusinessException;
//import com.renovatipoint.entities.concretes.Operation;
//import com.renovatipoint.enums.OperationType;
//import com.stripe.exception.StripeException;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/transactions")
//public class OperationController {
//    private final OperationBusinessManager operationBusinessManager;
//
//    public OperationController(OperationBusinessManager operationBusinessManager) {
//        this.operationBusinessManager = operationBusinessManager;
//    }
//
//
//    @PostMapping("/information-sharing")
//    public ResponseEntity<Operation> createInformationSharingTransaction(
//            @RequestParam String expertId,
//            @RequestParam String userId
//
//    ){
//        try {
//            Operation operation = operationBusinessManager.createInformationSharingTransaction(expertId, userId);
//            return ResponseEntity.ok(operation);
//        }catch (StripeException | BusinessException e){
//            return ResponseEntity.badRequest().body(null);
//        }
//
//    }
//
//    @PostMapping("/job-completion")
//    public ResponseEntity<Operation> createJobCompletionTransaction(
//            @RequestParam String expertId,
//            @RequestParam String userId,
//            @RequestParam String adId
//    ){
//        try {
//            Operation operation = operationBusinessManager.createJobCompletionTransaction(expertId, userId, adId);
//            return ResponseEntity.ok(operation);
//        }catch (StripeException | BusinessException e){
//         return ResponseEntity.badRequest().body(null);
//        }
//
//    }
//
//    @GetMapping("/expert/{expertId}")
//    public ResponseEntity<List<Operation>> getTransactionsByExpert(@PathVariable String expertId){
//        List<Operation> operations = operationBusinessManager.getTransactionsByExpert(expertId);
//        return ResponseEntity.ok(operations);
//    }
//
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<List<Operation>> getTransactionsByUser(@PathVariable String userId){
//        List<Operation> operations = operationBusinessManager.getTransactionsByUser(userId);
//        return ResponseEntity.ok(operations);
//    }
//
//    @GetMapping("/type/{type}")
//    public ResponseEntity<List<Operation>> getTransactionsByType(@PathVariable OperationType type){
//        List<Operation> operations = operationBusinessManager.getTransactionsByType(type);
//        return ResponseEntity.ok(operations);
//    }
//
////    @GetMapping("/date-range")
////    public ResponseEntity<List<Operation>> getTransactionsByDateRange(
////            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDateTime start,
////            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
////            ){
////        List<Operation> operations = operationManager.getTransactionsByDateRange(start, end);
////        return ResponseEntity.ok(operations);
////    }
//
//}
