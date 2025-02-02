package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.User;
import com.buildbetter.enums.Status;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    User getByEmail(String email);


    @NotNull
    Optional<User> findById(@NotNull String userId);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    List<User> findAllByStatus(Status status);

}
