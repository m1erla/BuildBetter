package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.business.responses.GetRequestsResponse;
import com.renovatipoint.entities.concretes.Ads;
import com.renovatipoint.entities.concretes.Request;
import com.renovatipoint.enums.RequestStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


public interface RequestRepository extends JpaRepository<Request, String> {

    List<Request> findByExpertId(String expertId);
    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.expert e " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.ad a " +
            "WHERE u.id = :userId")
    List<Request> findByUserId(@Param("userId") String userId);

    @Query("SELECT r FROM Request r " +
            "JOIN FETCH r.ad a " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.expert e " +
            "WHERE a.user.id = :userId")
    List<Request> findRequestsByAdOwner(@Param("userId") String userId);
    @NotNull
    @EntityGraph(attributePaths = {"expert", "ad", "ad.category"})
    Optional<Request> findById(@NotNull String id);
    List<Request> findByStatus(RequestStatus status);

    List<Request> findByAdAndStatus(Ads ad, RequestStatus status);
    List<Request> findByExpertIdAndStatus(String expertId, RequestStatus status);
}
