package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.concretes.SharedInformationManager;
import com.renovatipoint.entities.concretes.SharedInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/shared-information")
public class SharedInformationController {

    private final SharedInformationManager sharedInformationManager;

    @Autowired
    public SharedInformationController(SharedInformationManager sharedInformationManager){
        this.sharedInformationManager= sharedInformationManager;
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SharedInformation>> getSharedInformationByUser(@PathVariable String userId) {
        List<SharedInformation> sharedInfo = sharedInformationManager.getSharedInformationByUser(userId);
        return ResponseEntity.ok(sharedInfo);
    }

    @GetMapping("/expert/{expertId}")
    public ResponseEntity<List<SharedInformation>> getSharedInformationByExpert(@PathVariable String expertId) {
        List<SharedInformation> sharedInfo = sharedInformationManager.getSharedInformationByExpert(expertId);
        return ResponseEntity.ok(sharedInfo);
    }

    @GetMapping("/user/{userId}/expert/{expertId}")
    public ResponseEntity<List<SharedInformation>> getSharedInformationBetweenUserAndExpert(
            @PathVariable String userId,
            @PathVariable String expertId) {
        List<SharedInformation> sharedInfo = sharedInformationManager.getSharedInformationBetweenUserAndExpert(userId, expertId);
        return ResponseEntity.ok(sharedInfo);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<SharedInformation>> getSharedInformationByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end) {
        List<SharedInformation> sharedInfo = sharedInformationManager.getSharedInformationByDateRange(start, end);
        return ResponseEntity.ok(sharedInfo);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<SharedInformation>> getRecentSharedInformation(
            @RequestParam String userId,
            @RequestParam String expertId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date since) {
        List<SharedInformation> recentSharedInfo = sharedInformationManager.getRecentSharedInformation(userId, expertId, since);
        return ResponseEntity.ok(recentSharedInfo);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getSharedInformationCount(
            @RequestParam String userId,
            @RequestParam String expertId) {
        long count = sharedInformationManager.getSharedInformationCount(userId, expertId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/expert/{expertId}/uncharged/count")
    public ResponseEntity<Long> getUnchargedInformationCountForExpert(@PathVariable String expertId) {
        long count = sharedInformationManager.getUnchargedInformationCountForExpert(expertId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/expert/{expertId}/uncharged")
    public ResponseEntity<List<SharedInformation>> getUnchargedInformationForExpert(@PathVariable String expertId) {
        List<SharedInformation> unchargedInfo = sharedInformationManager.getUnchargedInformationForExpert(expertId);
        return ResponseEntity.ok(unchargedInfo);
    }
}
