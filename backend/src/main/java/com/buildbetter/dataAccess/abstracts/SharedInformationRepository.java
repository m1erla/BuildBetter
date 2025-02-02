package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.SharedInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface SharedInformationRepository extends JpaRepository<SharedInformation, String>
{
    List<SharedInformation> findByUserId(String userId);
List<SharedInformation> findByExpertId(String expertId);
List<SharedInformation> findByUserIdAndExpertId(String userId, String expertId);
List<SharedInformation> findBySharedAtBetween(Date start, Date end);

    @Query("SELECT si FROM SharedInformation si WHERE si.user.id = :userId AND si.expert.id = :expertId AND si.sharedAt >= :since")
    List<SharedInformation> findRecentSharedInformation(@Param("userId") String userId,
                                                        @Param("expertId") String expertId,
                                                        @Param("since") Date since);

    long countByUserIdAndExpertId(String userId, String expertId);

    @Query("SELECT COUNT(si) FROM SharedInformation si WHERE si.expert.id = :expertId AND si.charged = false")
    long countUnchargedInformationForExpert(@Param("expertId") String expertId);

    List<SharedInformation> findByExpertIdAndCharged(String expertId, boolean charged);

    @Query("SELECT si FROM SharedInformation si WHERE si.expert.id = :expertId AND si.charged = false ORDER BY si.sharedAt ASC")
    List<SharedInformation> findUnchargedInformationForExpertOrderBySharedAt(@Param("expertId") String expertId);
}

