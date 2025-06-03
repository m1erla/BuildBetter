package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.ServiceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, String> {

    boolean existsByName(String serviceName);

    // Kategori ID'ye göre servisleri getir
    List<ServiceEntity> findByCategoryId(String categoryId);

    // Kategori ID'ye göre servisleri getir ve belirtilen ID'yi hariç tut
    List<ServiceEntity> findByCategoryIdAndIdNot(String categoryId, String excludeId);

    // Kategori ID'ye göre servisleri getir ve sayfa boyutu sınırla
    List<ServiceEntity> findByCategoryId(String categoryId, Pageable pageable);

    // Kategori ID'ye göre servisleri getir, belirtilen ID'yi hariç tut ve sayfa
    // boyutu sınırla
    List<ServiceEntity> findByCategoryIdAndIdNot(String categoryId, String excludeId, Pageable pageable);
}
