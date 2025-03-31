package com.buildbetter.webApi.controllers;

import com.buildbetter.dataAccess.abstracts.UserRepository;
import com.buildbetter.entities.concretes.Role;
import com.buildbetter.entities.concretes.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin-access")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminAccessController {

    private final UserRepository userRepository;

    @Autowired
    public AdminAccessController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/promote/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> promoteToAdmin(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        user.setRole(Role.ADMIN);
        userRepository.save(user);

        return ResponseEntity.ok("User promoted to ADMIN role successfully");
    }

    @PostMapping("/demote/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> demoteFromAdmin(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Eğer kullanıcı ADMIN rolünde değilse, işlem yapmaya gerek yok
        if (user.getRole() != Role.ADMIN) {
            return ResponseEntity.ok("User is already not an ADMIN");
        }

        user.setRole(Role.USER);
        userRepository.save(user);

        return ResponseEntity.ok("User demoted from ADMIN role successfully");
    }

    @GetMapping("/check/{email}")
    public ResponseEntity<?> checkIfAdmin(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        return ResponseEntity.ok(isAdmin);
    }
}