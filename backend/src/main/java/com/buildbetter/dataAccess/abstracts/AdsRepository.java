package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.Ads;
import com.renovatipoint.entities.concretes.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public interface AdsRepository extends JpaRepository<Ads, String> {
    List<Ads> findByUserId(String userId);
    boolean existsByTitle(String title);
    boolean isActive(boolean isActive);


}
