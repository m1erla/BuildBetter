package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {



    boolean existsByName(String categoryName);

    Optional<Category> findByName(String categoryName);

}
