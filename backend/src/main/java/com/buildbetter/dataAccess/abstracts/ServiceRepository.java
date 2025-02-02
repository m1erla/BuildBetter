package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, String> {



    boolean existsByName(String serviceName);

}
