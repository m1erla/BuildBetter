package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.StorageService;
import com.buildbetter.business.responses.GetAllImagesResponse;
import com.buildbetter.core.utilities.images.ImageUtils;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.AdsRepository;
import com.buildbetter.dataAccess.abstracts.StorageRepository;
import com.buildbetter.dataAccess.abstracts.UserRepository;
import com.buildbetter.entities.concretes.Ads;
import com.buildbetter.entities.concretes.Storage;
import com.buildbetter.entities.concretes.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StorageManager implements StorageService {
    private static final String UPLOAD_DIR = "backend/src/main/resources/static/uploads";
    private final Path rootLocation = Paths.get(UPLOAD_DIR);
    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);
    private final StorageRepository storageRepository;

    private final ModelMapperService modelMapperService;
    private final AdsRepository adsRepository;
    private final UserRepository userRepository;

    @Autowired
    public StorageManager(StorageRepository storageRepository, ModelMapperService modelMapperService,
            AdsRepository adsRepository, UserRepository userRepository) {
        this.storageRepository = storageRepository;
        this.modelMapperService = modelMapperService;
        this.adsRepository = adsRepository;
        this.userRepository = userRepository;
        initializeStorageDirectory();
    }
    private void initializeStorageDirectory() {
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
                logger.info("Created storage directory: {}", rootLocation);
            }
        } catch (IOException e) {
            logger.error("Could not initialize storage directory: {}", rootLocation, e);
        }
    }


    // Dosyayı diske kaydeder ve dosya adını döndürür
    @Override
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IOException("Cannot store empty file.");
        String uniqueFilename = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("\\s+", "_");
        Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize();
        if (!destinationFile.getParent().equals(this.rootLocation)) throw new IOException("Cannot store outside directory.");
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored file: {}", uniqueFilename);
            return uniqueFilename;
        } catch (IOException e) {
            logger.error("Failed to store file {}: {}", uniqueFilename, e.getMessage());
            throw new IOException("Failed to store file " + uniqueFilename, e);
        }
    }

    // **ÖNEMLİ DEĞİŞİKLİK: List<Storage> döndürür**
    @Override
    @Transactional
    public List<Storage> uploadImages(List<MultipartFile> files, User user, Ads ad) throws IOException {
        List<Storage> savedStorages = new ArrayList<>();
        if (files == null || files.isEmpty()) return savedStorages;

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String fileName = storeFile(file); // Fiziksel kaydetme

            Storage storageData = Storage.builder()
                    .name(fileName)
                    .type(file.getContentType())
                    .url(buildImageUrl(ad.getId(), fileName)) // URL oluştur
                    .user(user) // İlişkili kullanıcı
                    .ads(ad)    // İlişkili ilan
                    .build();

            Storage savedStorage = storageRepository.save(storageData); // DB'ye kaydet
            savedStorages.add(savedStorage); // Listeye ekle
            logger.info("Saved ad image metadata for ad {} with storage ID {}", ad.getId(), savedStorage.getId());
        }
        return savedStorages; // Kaydedilen entity listesini döndür
    }

    // **ÖNEMLİ DEĞİŞİKLİK: Storage döndürür**
    @Override
    @Transactional
    public Storage uploadImage(MultipartFile file, User user) throws IOException {
        String fileName = storeFile(file);
        Storage storageData = Storage.builder()
                .name(fileName)
                .type(file.getContentType())
                .url(buildImageUrl(null, fileName)) // Kullanıcı resmi için URL (adId=null)
                .user(user)
                .build();
        Storage savedStorage = storageRepository.save(storageData);
        logger.info("Saved user profile image metadata for user {} with storage ID {}", user.getId(), savedStorage.getId());
        return savedStorage;
    }


    // Sadece fiziksel dosyayı siler
    @Override
    public void deleteImage(String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("Attempted to delete image with null or blank filename.");
            return;
        }
        try {
            Path filePath = rootLocation.resolve(fileName).normalize();
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) logger.info("Deleted image file: {}", fileName);
            else logger.warn("Image file not found, could not delete: {}", fileName);
        } catch (IOException e) {
            logger.error("Failed to delete image file {}: {}", fileName, e.getMessage());
            throw new IOException("Could not delete file: " + fileName, e);
        }
    }

    // loadAsResource (StorageService'den @Override eklenmeli)
    @Override
    public Resource loadAsResource(String filename) throws MalformedURLException {
        if (filename == null || filename.isBlank()) return null;
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                logger.warn("Resource does not exist or is not readable: {}", file.toString());
                return null;
            }
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException for file {}: {}", filename, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error loading resource for file {}: {}", filename, e.getMessage());
            return null;
        }
    }
    @Override
    @Transactional
    public byte[] downloadImage(String fileName) throws IOException {
        Optional<Storage> dbImageData = storageRepository.findByName(fileName);
        if (dbImageData != null) {
            return ImageUtils.decompressImage(dbImageData.get().getImageData());
        } else {
            throw new FileNotFoundException("File not found with name: " + fileName);
        }
    }

    /*************************************
     * Storage Service Implementations
     *************************************/
    // Serves an image file from the filesystem
    @Override
    @Transactional(readOnly = true) // Read-only transaction for serving images
    public ResponseEntity<?> serveImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("Attempted to serve image with null or blank filename.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid filename");
        }
        try {
            Resource resource = loadAsResource(fileName); // Load resource
            if (resource == null || !resource.exists() || !resource.isReadable()) {
                logger.warn("Image resource not found or not readable: {}", fileName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found");
            }
            String contentType = determineContentType(fileName); // Determine content type
            logger.debug("Serving image {} with content type {}", fileName, contentType);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (MalformedURLException e) {
            logger.error("Malformed URL exception for file {}: {}", fileName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not determine file path");
        } catch (Exception e) {
            logger.error("Error serving image {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error loading image");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetAllImagesResponse> getAll() {
        List<Storage> storages = storageRepository.findAll();
        return storages.stream()
                .map(image -> this.modelMapperService.forResponse().map(image, GetAllImagesResponse.class))
                .collect(Collectors.toList());
    }
    // Helper to build image URL dynamically
    private String buildImageUrl(String adId, String imageName) {
        // Get base URL from properties or env var if possible
        String baseUrl = "http://localhost:8080"; // Default
        if (adId != null) {
            return baseUrl + "/api/v1/ads/" + adId + "/images/" + imageName;
        } else {
            // Build a URL for user profile images if needed, or a generic one
            // This endpoint needs to exist in UserController or similar
            return baseUrl + "/api/v1/users/images/" + imageName; // Example
        }
    }

    // Helper to determine content type
    private String determineContentType(String filename) {
        // Same as before
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "application/octet-stream";
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "jpg": case "jpeg": return "image/jpeg";
            case "webp": return "image/webp";
            default: return "application/octet-stream";
        }
    }

}
