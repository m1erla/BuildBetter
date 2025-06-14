package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.Storage;
import com.buildbetter.entities.concretes.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageRepository extends JpaRepository<Storage, String> {

    Optional<Storage> findByName(String name);

    List<Storage> findByUserAndName(User user, String name);

    @NotNull
    Optional<Storage> findById(@NotNull String storageId);

    void deleteByName(String name);


    Optional<Storage> findByNameAndUserId(String oldFileName, String id);
}
