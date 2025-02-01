package com.renovatipoint.business.concretes;

import com.renovatipoint.business.abstracts.UserService;
import com.renovatipoint.business.requests.ChangePasswordRequest;
import com.renovatipoint.business.requests.UpdateUserRequest;
import com.renovatipoint.business.responses.*;
import com.renovatipoint.business.rules.UserBusinessRules;
import com.renovatipoint.core.utilities.mappers.ModelMapperService;
import com.renovatipoint.dataAccess.abstracts.StorageRepository;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import com.renovatipoint.entities.concretes.Invoice;
import com.renovatipoint.entities.concretes.Storage;
import com.renovatipoint.entities.concretes.User;
import com.renovatipoint.enums.Status;
import com.renovatipoint.security.token.Token;
import com.renovatipoint.security.token.TokenRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserManager implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);
    private final ModelMapperService modelMapperService;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;

    private final PasswordEncoder passwordEncoder;
    private final StorageManager storageManager;
    private final AdsManager adsManager;
    private final TokenRepository tokenRepository;

    private JavaMailSender mailSender;

    public UserManager(ModelMapperService modelMapperService, UserRepository userRepository,
            UserBusinessRules userBusinessRules, StorageRepository storageRepository, PasswordEncoder passwordEncoder,
            StorageManager storageManager, AdsManager adsManager, TokenRepository tokenRepository) {
        this.modelMapperService = modelMapperService;
        this.userRepository = userRepository;
        this.storageRepository = storageRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageManager = storageManager;
        this.adsManager = adsManager;
        this.tokenRepository = tokenRepository;
    }

    @Override
    public List<GetUsersResponse> getAll() {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> this.modelMapperService
                .forResponse()
                .map(user, GetUsersResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public GetUsersResponse getUserByEmail(String email) {
        Optional<User> user = this.userRepository.findByEmail(email);

        return this.modelMapperService.forResponse().map(user, GetUsersResponse.class);
    }

    @Override
    public GetExpertResponse getExpertByEmail(String email) {
        Optional<User> user = this.userRepository.findByEmail(email);

        return this.modelMapperService.forResponse().map(user, GetExpertResponse.class);
    }

    @Override
    public User getByEmail(String email) {
        User user = this.userRepository.getByEmail(email);
        return this.modelMapperService.forResponse().map(user, User.class);
    }

    @Override
    public GetUsersResponse getById(String userId) {
        User user = this.userRepository.findById(userId).orElseThrow();

        return this.modelMapperService.forResponse().map(user, GetUsersResponse.class);
    }

    @Override
    @Transactional
    public ResponseEntity<?> getUserProfileImage(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        String fileName = user.getProfileImage();
        if (fileName == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile image not found");
        }

        return storageManager.serveImage(fileName);
    }

    @Override
    @Transactional
    public ResponseEntity<?> getImageWithFileName(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        String fileName = user.getProfileImage();
        if (fileName == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile image not found");
        }
        return ResponseEntity.ok(new GetAllImagesResponse());
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UpdateUserRequest updateUserRequest) {
        try {
            User user = userRepository.findById(updateUserRequest.getId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            // Manuel olarak alanları güncelle
            if (updateUserRequest.getName() != null)
                user.setName(updateUserRequest.getName());
            if (updateUserRequest.getSurname() != null)
                user.setSurname(updateUserRequest.getSurname());
            if (updateUserRequest.getEmail() != null)
                user.setEmail(updateUserRequest.getEmail());
            if (updateUserRequest.getPhoneNumber() != null)
                user.setPhoneNumber(updateUserRequest.getPhoneNumber());
            if (updateUserRequest.getAddress() != null)
                user.setAddress(updateUserRequest.getAddress());
            if (updateUserRequest.getPostCode() != null)
                user.setPostCode(updateUserRequest.getPostCode());
            if (updateUserRequest.getRole() != null)
                user.setRole(updateUserRequest.getRole());

            // Profil resmi güncelleme
            if (updateUserRequest.getStorages() != null && !updateUserRequest.getStorages().isEmpty()) {
                try {
                    String oldFileName = user.getProfileImage();
                    List<Storage> oldStorage = user.getStorages();
                    if (oldFileName != null && !oldFileName.isEmpty() && oldStorage != null) {
                        storageManager.deleteImage(oldFileName);
                        storageRepository.deleteAll(oldStorage);
                    }
                    String newFileName = storageManager.uploadImage(updateUserRequest.getStorages(), user);
                    user.setProfileImage(newFileName);
                } catch (IOException ex) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to update user profile image");
                }
            }

            // Kullanıcıyı kaydet
            User updatedUser = userRepository.save(user);

            // Güncellenmiş kullanıcı bilgilerini response olarak dön
            GetUsersResponse response = modelMapperService.forResponse()
                    .map(updatedUser, GetUsersResponse.class);

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found with ID: " + updateUserRequest.getId());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating user: " + e.getMessage());
        }
    }

    public ResponseEntity<?> changePassword(ChangePasswordRequest request, Principal connectedUser) {
        UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken) connectedUser;

        User user = (User) authenticationToken.getPrincipal();

        logger.info("Initiating password change for user: {}", user.getUsername());

        // check if the current password is correct
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Wrong current password for user: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong password");
        }

        // check if the new password is the same as the current password
        if (request.getPassword().equals(request.getNewPassword())) {
            logger.warn("New password is the same as the old password for user: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("New password cannot be the same as the old password. Please try a different password.");
        }

        // check if the two new passwords match
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            logger.warn("New password and confirmation password do not match for user: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords do not match");
        }

        // update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        logger.info("Password changed successfully for user: {}", user.getUsername());
        return ResponseEntity.ok().body("Password changed successfully");
    }

    @Override
    @Transactional
    public ResponseEntity<?> uploadUserProfileImage(MultipartFile file, String id) throws IOException {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        String oldProfileImage = user.getProfileImage();
        List<Storage> oldProfileStorages = storageRepository.findByUserAndName(user, oldProfileImage);

        // Delete old images from the file system and database
        if (!oldProfileStorages.isEmpty() && oldProfileImage != null) {
            for (Storage storage : oldProfileStorages) {
                storageManager.deleteImage(storage.getName());
                storageRepository.delete(storage);
            }
        }

        String newFileName = storageManager.uploadImage(file, user);
        user.setProfileImage(newFileName);
        userRepository.save(user);

        return ResponseEntity.ok("Profile image uploaded successfully: " + newFileName);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteUserProfileImage(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));

        try {
            if (user.getProfileImage() != null) {
                storageManager.deleteImage(user.getProfileImage());
            }

            List<Storage> storages = user.getStorages();
            if (storages != null) {
                for (Storage storage : storages) {
                    storageRepository.delete(storage);
                }
                storages.clear();
            }

            List<Token> tokens = user.getToken();
            if (tokens != null) {
                for (Token token : tokens) {
                    tokenRepository.delete(token);
                }
                tokens.clear();
            }

            userRepository.delete(user);

            return ResponseEntity.ok("User profile image and related entities deleted successfully.");
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete user profile image.");
        }
    }

    @Override
    public void sendPaymentConfirmationEmail(String userEmail, Invoice invoice) {
        String subject = "Payment Confirmation";
        String message = "Dear user your payment for invoice " + invoice.getInvoiceNumber()
                + " was successful. Thank you!";
        sendEmail(userEmail, subject, message);
    }

    @Override
    public void sendPaymentFailureEmail(String userEmail, Invoice invoice) {
        String subject = "Payment Failure Notification";
        String message = "Dear user, your payment for invoice " + invoice.getInvoiceNumber()
                + " has failed. Please try again or contact support.";
        sendEmail(userEmail, subject, message);
    }

    @Override
    public void saveUser(User user) {
        user.setStatus(Status.ONLINE);
        userRepository.save(user);
    }

    @Override
    public void disconnect(User user) {
        var storedUser = userRepository.findById(user.getId()).orElse(null);
        if (storedUser != null) {
            storedUser.setStatus(Status.OFFLINE);
            userRepository.save(storedUser);
        }
    }

    @Override
    public List<User> findConnectedUsers() {
        return userRepository.findAllByStatus(Status.ONLINE);
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}

// public User findCurrentUser(String email) {
// Optional<User> loggedInUser = userRepository.findByEmail(email);
//
// // or throw an exception
// return loggedInUser.flatMap(user ->
// userRepository.findById(user.getId())).orElse(null);
//
// }
// @Override
// public UserDetails loadUserByUsername(String username) throws
// UsernameNotFoundException {
// Optional<User> user = userRepository.findByEmail(username);
//
// if (user.isEmpty()){
// throw new UsernameNotFoundException("User not found with this email - " +
// username);
// }
//
//
// return new
// org.springframework.security.core.userdetails.User(findCurrentUser().getEmail(),
// findCurrentUser().getPassword(), findCurrentUser().getAuthorities());
//
// }

// @Override
// public User getUserByJwt(String jwt) {
// Optional<User> userOptional = this.userRepository.findByEmail(jwt);
//
// if (userOptional.isPresent()) {
// User user = userOptional.get();
// User response = this.modelMapperService.forResponse().map(user, User.class);
// return response;
// } else {
// // Handle the case where no user is found with the provided email (jwt)
// // You can throw a specific exception, return null, or handle it according to
// your needs.
// throw new BusinessException("User not found with the provided JWT");
// }
// }
