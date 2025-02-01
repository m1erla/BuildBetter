package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.*;
import com.renovatipoint.business.concretes.StorageManager;
import com.renovatipoint.business.concretes.UserManager;
import com.renovatipoint.business.requests.*;
import com.renovatipoint.business.responses.*;
import com.renovatipoint.entities.concretes.User;
import com.renovatipoint.security.auth.AuthenticationService;
import com.renovatipoint.security.jwt.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spark.Response;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    private final UserManager userManager;

    private final StorageManager storageManager;
    private final AuthenticationService service;
    private final JwtService jwtService;

    @GetMapping
    public List<GetUsersResponse> getAllUsers() {
        return userManager.getAll();
    }

    @GetMapping("/{id}")
    public GetUsersResponse getUsersById(@PathVariable String id) {
        return userManager.getById(id);
    }

    @GetMapping(value = "/{id}/profile-image")
    public ResponseEntity<?> getUserProfileImage(@PathVariable String id) {
        return userManager.getUserProfileImage(id);
    }

    @GetMapping("/response")
    public ResponseEntity<GetUsersResponse> retrieveUserProfileWithResponse(
            @RequestHeader("Authorization") String authorizationHeader) {
        // Extract the token from the Authorization header (remove "Bearer " prefix)
        String jwt = authorizationHeader.substring(7).trim();

        String email = jwtService.extractUsername(jwt);
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/current")
    public ResponseEntity<UserDTO> getCurrentUser(Principal principal) {
        User user = userManager.getByEmail(principal.getName());
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    @PatchMapping
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request, Principal connectedUser) {
        return userManager.changePassword(request, connectedUser);
    }

    @PostMapping("/{id}/profile-image")
    public ResponseEntity<?> uploadUserProfileImage(@RequestParam("file") MultipartFile file, @PathVariable User id)
            throws IOException {
        return userManager.uploadUserProfileImage(file, id.getId());
    }

    @PostMapping("{id}/uploadProfileImage")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file, @PathVariable String id) {
        try {
            ResponseEntity<?> message = userManager.uploadUserProfileImage(file, id);
            return ResponseEntity.ok(message);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Failed to upload profile image");
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@ModelAttribute UpdateUserRequest updateUserRequest) throws IOException {
        try {
            // Token'dan kullanıcı bilgilerini al
            String userId = updateUserRequest.getId();
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.badRequest().body("User ID is required");
            }

            return userManager.update(updateUserRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating user: " + e.getMessage());
        }
    }

    @DeleteMapping("/{fileName}/profile-image")
    public void deleteUserProfileImage(@PathVariable String fileName) throws IOException {
        storageManager.deleteImage(fileName);
    }

    @MessageMapping("/user.addUser")
    @SendTo("/user/public")
    public User addUser(@Payload User user) {
        userManager.saveUser(user);
        return user;
    }

    @MessageMapping("/user.disconnectUser")
    @SendTo("/user/public")
    public User disconnectUser(@Payload User user) {
        userManager.disconnect(user);
        return user;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> findConnectedUsers() {
        return ResponseEntity.ok(userManager.findConnectedUsers());
    }

}
