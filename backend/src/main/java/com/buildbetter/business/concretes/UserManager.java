package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.UserService;
import com.buildbetter.business.requests.ChangePasswordRequest;
import com.buildbetter.business.requests.UpdateUserRequest;
import com.buildbetter.business.responses.*;
import com.buildbetter.business.rules.UserBusinessRules;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.StorageRepository;
import com.buildbetter.dataAccess.abstracts.UserRepository;
import com.buildbetter.entities.concretes.Invoice;
import com.buildbetter.entities.concretes.Storage;
import com.buildbetter.entities.concretes.User;
import com.buildbetter.enums.Status;
import com.buildbetter.security.token.Token;
import com.buildbetter.security.token.TokenRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.buildbetter.core.utilities.exceptions.BusinessException;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
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
            // ID null veya boş olamaz
            if (updateUserRequest.getId() == null || updateUserRequest.getId().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User ID must be provided for update.");
            }

            User user = userRepository.findById(updateUserRequest.getId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("User not found with ID: " + updateUserRequest.getId()));

            // --- YETKİ KONTROLÜ BAŞLANGICI ---
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                logger.warn("Attempt to update user {} without authentication.", updateUserRequest.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
            }

            String currentPrincipalName = authentication.getName(); // Mevcut kullanıcının email'i

            // Admin kontrolü
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            // Sahip kontrolü (Email ile)
            boolean isOwner = user.getEmail().equalsIgnoreCase(currentPrincipalName);

            // Eğer kullanıcı admin değilse VE profilin sahibi değilse, erişimi engelle
            if (!isAdmin && !isOwner) {
                logger.warn("Access denied for user {} attempting to update user {} ({})", currentPrincipalName,
                        user.getId(), user.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to update this user profile.");
            }
            logger.info("User {} authorized to update user {}", currentPrincipalName, user.getId());
            // --- YETKİ KONTROLÜ SONU ---

            // Manuel olarak alanları güncelle (Email güncellemesi genellikle yapılmaz veya
            // çok dikkatli yapılır)
            if (updateUserRequest.getName() != null)
                user.setName(updateUserRequest.getName());
            if (updateUserRequest.getSurname() != null)
                user.setSurname(updateUserRequest.getSurname());
            // Email güncellemesi gerekiyorsa, benzersizlik kontrolü ve güvenlik etkileri
            // düşünülmeli.
            // if (updateUserRequest.getEmail() != null &&
            // !updateUserRequest.getEmail().equalsIgnoreCase(user.getEmail())) {
            // logger.warn("Email update attempt for user {} by {}", user.getId(),
            // currentPrincipalName);
            // // Email güncelleme mantığı ve kontrolleri buraya...
            // // user.setEmail(updateUserRequest.getEmail());
            // }
            if (updateUserRequest.getPhoneNumber() != null)
                user.setPhoneNumber(updateUserRequest.getPhoneNumber());
            if (updateUserRequest.getAddress() != null)
                user.setAddress(updateUserRequest.getAddress());
            if (updateUserRequest.getPostCode() != null)
                user.setPostCode(updateUserRequest.getPostCode());

            // Rol güncellemesi sadece ADMIN tarafından yapılabilmeli
            if (updateUserRequest.getRole() != null && !user.getRole().equals(updateUserRequest.getRole())) {
                if (isAdmin) {
                    logger.info("Admin {} updating role for user {} to {}", currentPrincipalName, user.getId(),
                            updateUserRequest.getRole());
                    user.setRole(updateUserRequest.getRole());
                } else {
                    logger.warn("User {} attempted to change role for user {} without ADMIN privileges.",
                            currentPrincipalName, user.getId());
                    // Hata döndür veya sadece logla
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("You do not have permission to change the user role.");
                }
            }

            // Profil Resmi Güncelleme
            MultipartFile newProfileImageFile = updateUserRequest.getProfileImageFile();
            if (newProfileImageFile != null && !newProfileImageFile.isEmpty()) {
                logger.info("Updating profile image for user {}", user.getId());
                String oldFileName = user.getProfileImage();

                // 1. Eski resmi sil (Yardımcı metodu kullan)
                if (oldFileName != null && !oldFileName.isBlank()) {
                    try {
                        deleteUserProfileStorage(user, oldFileName);
                    } catch (BusinessException e) {
                        // deleteUserProfileStorage içinde loglama yapılıyor, burada tekrar loglamaya
                        // gerek yok.
                        // Ancak işlemi durdurmak isteyebiliriz.
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to remove old profile image: " + e.getMessage());
                    }
                }

                // 2. Yeni resmi yükle
                try {
                    Storage newStorage = storageManager.uploadImage(newProfileImageFile, user);
                    user.setProfileImage(newStorage.getName());
                    // İlişki yönetimi (Eğer User -> Storage @OneToMany(cascade = ..., orphanRemoval
                    // = true) ise bu kısım gereksiz olabilir)
                    if (user.getStorages() == null)
                        user.setStorages(new ArrayList<>());
                    // Eğer storageManager.uploadImage DB'ye kaydetmiyorsa veya cascade yoksa
                    // eklemek gerekebilir:
                    // storageRepository.save(newStorage); // Gerekli mi kontrol edilmeli.
                    user.getStorages().add(newStorage); // Bu satır cascade varsa ve save yapılıyorsa gereksiz olabilir.
                    logger.info("Successfully updated profile image for user {} to {}", user.getId(),
                            newStorage.getName());
                } catch (IOException ex) {
                    logger.error("Failed to upload new profile image for user {}: {}", user.getId(), ex.getMessage());
                    throw new BusinessException("Failed to update profile image file.");
                }
            }

            User updatedUser = userRepository.save(user);
            GetUsersResponse response = modelMapperService.forResponse().map(updatedUser, GetUsersResponse.class);
            logger.info("User profile updated successfully for ID: {}", updatedUser.getId());
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            logger.warn("User update failed - Not Found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) { // Yetki kontrolü için eklendi
            logger.warn("User update failed - Access Denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (BusinessException e) {
            logger.warn("User update failed - Business Rule Violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected error occurred during user update for ID {}: {}",
                    updateUserRequest != null ? updateUserRequest.getId() : "unknown",
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while updating the user profile.");
        }
    }

    public ResponseEntity<?> changePassword(ChangePasswordRequest request, Principal connectedUser) {
        UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken) connectedUser;
        User user = (User) authenticationToken.getPrincipal(); // Get User object

        // Güvenlik loglaması
        logger.info("Password change initiated by user: {}", user.getEmail());

        // Mevcut kontroller...
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Incorrect current password provided by user: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong password"); // UNAUTHORIZED daha uygun
        }
        if (request.getPassword().equals(request.getNewPassword())) {
            logger.warn("User {} attempted to set the same password.", user.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("New password cannot be the same as the old password."); // BAD_REQUEST daha uygun
        }
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            logger.warn("Password confirmation mismatch for user: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        logger.info("Password successfully changed for user: {}", user.getEmail());
        return ResponseEntity.ok("Password changed successfully"); // Body mesajı düzeltildi
    }

    @Override
    @Transactional
    public ResponseEntity<?> uploadUserProfileImage(MultipartFile file, String id) throws IOException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        // --- YETKİ KONTROLÜ BAŞLANGICI ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("Attempt to upload profile image for user {} without authentication.", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }
        String currentPrincipalName = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = user.getEmail().equalsIgnoreCase(currentPrincipalName);
        if (!isAdmin && !isOwner) {
            logger.warn("Access denied for user {} attempting to upload image for user {} ({})", currentPrincipalName,
                    user.getId(), user.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to upload an image for this user.");
        }
        logger.info("User {} authorized to upload image for user {}", currentPrincipalName, user.getId());
        // --- YETKİ KONTROLÜ SONU ---

        String oldProfileImage = user.getProfileImage();
        // Eski resim silme mantığı (deleteUserProfileStorage çağrılabilir)
        if (oldProfileImage != null && !oldProfileImage.isBlank()) {
            try {
                deleteUserProfileStorage(user, oldProfileImage);
                logger.info("Successfully deleted old profile image {} for user {}", oldProfileImage, id);
            } catch (BusinessException e) {
                logger.error("Could not delete old profile image {} for user {}: {}", oldProfileImage, id,
                        e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to remove old profile image before uploading new one.");
            }
        }

        // Yeni resmi yükle
        try {
            Storage newStorage = storageManager.uploadImage(file, user);
            user.setProfileImage(newStorage.getName()); // Sadece ismi set ediyoruz.
            // İlişki yönetimi (gerekliyse)
            if (user.getStorages() == null)
                user.setStorages(new ArrayList<>());
            user.getStorages().add(newStorage); // Cascade ile yönetilmiyorsa veya save yapılmıyorsa
            userRepository.save(user);
            logger.info("Successfully uploaded new profile image {} for user {}", newStorage.getName(), id);
            return ResponseEntity.ok("Profile image uploaded successfully: " + newStorage.getName());
        } catch (IOException e) {
            logger.error("Failed to upload profile image for user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload profile image.");
        }
    }

    // deleteUserProfileStorage - Bu metot içeriden çağrılıyor, çağrıldığı yerde
    // yetki kontrolü yapılıyor.
    // Ancak doğrudan API endpoint'i varsa, buraya da kontrol eklemek GEREKİR.
    // Şimdilik içeriden çağrıldığını varsayarak dokunmuyoruz ama endpoint varsa
    // DİKKAT!
    private void deleteUserProfileStorage(User user, String fileNameToDelete) throws BusinessException { // throws
                                                                                                         // BusinessException
                                                                                                         // eklendi
        if (fileNameToDelete == null || fileNameToDelete.isBlank())
            return;

        Optional<Storage> storageOpt = storageRepository.findByNameAndUserId(fileNameToDelete, user.getId());
        if (storageOpt.isPresent()) {
            Storage storage = storageOpt.get();
            try {
                storageManager.deleteImage(storage.getName()); // Sadece isimle sil
                logger.info("Deleted image file {} for user {}", storage.getName(), user.getId());
            } catch (IOException e) {
                logger.error("Error deleting image file {} for user {}: {}", storage.getName(), user.getId(),
                        e.getMessage());
                // Dosya silinemese bile DB kaydını silmeyi deneyebiliriz veya hatayı yukarı
                // fırlatabiliriz.
                throw new BusinessException("Could not delete image file: " + storage.getName());
            }
            try {
                // İlişkiyi User tarafından yönetiyorsak (orphanRemoval=true) bu gereksiz.
                // user.getStorages().remove(storage); // ConcurrentModification hatası
                // verebilir
                storageRepository.delete(storage);
                logger.info("Deleted storage record {} for user {}", storage.getName(), user.getId());
            } catch (Exception e) {
                logger.error("Error deleting storage record {} for user {}: {}", storage.getName(), user.getId(), e);
                throw new BusinessException("Could not delete image database record: " + storage.getName());
            }
        } else {
            logger.warn("Storage record not found for file name {} and user {}. Attempting to delete file only.",
                    fileNameToDelete, user.getId());
            try {
                storageManager.deleteImage(fileNameToDelete);
                logger.info("Deleted orphan image file {} for user {}", fileNameToDelete, user.getId());
            } catch (IOException e) {
                logger.warn("Could not delete orphan image file {} for user {}: {}", fileNameToDelete, user.getId(),
                        e.getMessage());
                // Kayıt yoksa dosya silme hatası genellikle kritik değildir.
            }
        }
        // Kullanıcının profil resmini null yap (eğer silinen resim o ise)
        if (fileNameToDelete.equals(user.getProfileImage())) {
            user.setProfileImage(null);
            // user nesnesi transactional context'te olduğu için save'e gerek olmayabilir,
            // ama emin olmak için save çağrılabilir (çağıran metot save yapıyor zaten)
            // userRepository.save(user);
        }
    }

    // deleteUserProfileImage - Bu public metot ve muhtemelen API endpoint'i var.
    // Yetki kontrolü eklenmeli.
    @Override
    @Transactional
    public ResponseEntity<?> deleteUserProfileImage(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        String fileNameToDelete = user.getProfileImage();

        if (fileNameToDelete == null || fileNameToDelete.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User does not have a profile image to delete.");
        }

        // --- YETKİ KONTROLÜ BAŞLANGICI ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("Attempt to delete profile image for user {} without authentication.", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }
        String currentPrincipalName = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = user.getEmail().equalsIgnoreCase(currentPrincipalName);
        if (!isAdmin && !isOwner) {
            logger.warn("Access denied for user {} attempting to delete image for user {} ({})", currentPrincipalName,
                    user.getId(), user.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to delete the profile image for this user.");
        }
        logger.info("User {} authorized to delete image for user {}", currentPrincipalName, user.getId());
        // --- YETKİ KONTROLÜ SONU ---

        try {
            deleteUserProfileStorage(user, fileNameToDelete);
            // User'ı tekrar kaydetmeye gerek var mı? deleteUserProfileStorage içindeki
            // save(user) kaldırıldı.
            // Profil resmi null yapıldığı için kaydedilmeli.
            user.setProfileImage(null); // Bu satır deleteUserProfileStorage'a taşınabilir veya burada kalabilir.
            userRepository.save(user);
            logger.info("Successfully deleted profile image {} for user {}", fileNameToDelete, id);
            return ResponseEntity.ok("Profile image deleted successfully.");
        } catch (BusinessException e) {
            logger.error("Failed to delete profile image {} for user {}: {}", fileNameToDelete, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete profile image: " + e.getMessage());
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
