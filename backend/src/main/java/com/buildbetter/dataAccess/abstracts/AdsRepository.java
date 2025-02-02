package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.Ads;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdsRepository extends JpaRepository<Ads, String> {
    List<Ads> findByUserId(String userId);
    boolean existsByTitle(String title);
    boolean isActive(boolean isActive);


}
