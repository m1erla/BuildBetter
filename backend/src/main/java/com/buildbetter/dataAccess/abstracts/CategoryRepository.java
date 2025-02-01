package com.renovatipoint.dataAccess.abstracts;

import com.renovatipoint.entities.concretes.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {



    boolean existsByName(String categoryName);

    Optional<Category> findByName(String categoryName);

}
