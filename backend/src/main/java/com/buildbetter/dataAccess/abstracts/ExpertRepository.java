package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.Expert;
import com.renovatipoint.entities.concretes.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExpertRepository extends JpaRepository<Expert, String> {


    Optional<Expert> findByEmail(String email);

    @NotNull
    Optional<Expert> findById(@NotNull String expertId);


    @Query("SELECT e FROM Expert e WHERE e.paymentIssuesCount > 0 AND e.accountBlocked = false")
    List<Expert> findAllWithPaymentIssues();
}
