package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.JobTitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobTitleRepository extends JpaRepository<JobTitle, String> {


    boolean existsByName(String jobTitleName);

    Optional<JobTitle> findByName(String name);



}
