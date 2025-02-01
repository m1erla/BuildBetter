package com.renovatipoint.business.concretes;

import com.renovatipoint.dataAccess.abstracts.SharedInformationRepository;
import com.renovatipoint.entities.concretes.SharedInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class SharedInformationManager {
    private final SharedInformationRepository sharedInformationRepository;
    @Autowired
    public SharedInformationManager(SharedInformationRepository sharedInformationRepository) {
        this.sharedInformationRepository = sharedInformationRepository;
    }

    public List<SharedInformation> getSharedInformationByUser(String userId) {
        return sharedInformationRepository.findByUserId(userId);
    }

    public List<SharedInformation> getSharedInformationByExpert(String expertId) {
        return sharedInformationRepository.findByExpertId(expertId);
    }

    public List<SharedInformation> getSharedInformationBetweenUserAndExpert(String userId, String expertId) {
        return sharedInformationRepository.findByUserIdAndExpertId(userId, expertId);
    }

    public List<SharedInformation> getSharedInformationByDateRange(Date start, Date end) {
        return sharedInformationRepository.findBySharedAtBetween(start, end);
    }

    public List<SharedInformation> getRecentSharedInformation(String userId, String expertId, Date since) {
        return sharedInformationRepository.findRecentSharedInformation(userId, expertId, since);
    }

    public long getSharedInformationCount(String userId, String expertId) {
        return sharedInformationRepository.countByUserIdAndExpertId(userId, expertId);
    }

    public long getUnchargedInformationCountForExpert(String expertId) {
        return sharedInformationRepository.countUnchargedInformationForExpert(expertId);
    }

    public List<SharedInformation> getUnchargedInformationForExpert(String expertId) {
        return sharedInformationRepository.findUnchargedInformationForExpertOrderBySharedAt(expertId);
    }

}
