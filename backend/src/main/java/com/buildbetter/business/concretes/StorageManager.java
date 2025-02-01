package com.renovatipoint.business.concretes;

import com.renovatipoint.business.abstracts.StorageService;
import com.renovatipoint.business.responses.GetAllImagesResponse;
import com.renovatipoint.core.utilities.images.ImageUtils;
import com.renovatipoint.core.utilities.mappers.ModelMapperService;
import com.renovatipoint.dataAccess.abstracts.AdsRepository;
import com.renovatipoint.dataAccess.abstracts.StorageRepository;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import com.renovatipoint.entities.concretes.Ads;
import com.renovatipoint.entities.concretes.Storage;
import com.renovatipoint.entities.concretes.User;
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
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads";

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
    }

    @Override
    @Transactional
    public String storeFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        // String randomID = UUID.randomUUID().toString();
        // String randomName =
        // randomID.concat(fileName.substring(fileName.lastIndexOf(".")));
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    @Override
    @Transactional
    public String uploadImage(MultipartFile file, User user) throws IOException {
        String fileName = storeFile(file);
        Storage storageData = Storage.builder()
                .name(fileName)
                .type(file.getContentType())
                .imageData(ImageUtils.compressImage(file.getBytes()))
                .url(UPLOAD_DIR)
                .user(user)
                .build();

        storageRepository.save(storageData);

        return fileName;
    }

    @Override
    @Transactional
    public List<String> uploadImages(List<MultipartFile> files, User user, Ads ad) throws IOException {
        List<String> fileNames = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = storeFile(file);

            Storage storageData = Storage.builder()
                    .name(fileName)
                    .type(file.getContentType())
                    .imageData(ImageUtils.compressImage(file.getBytes()))
                    .url(UPLOAD_DIR)
                    .user(user)
                    .ads(ad)
                    .build();

            storageRepository.save(storageData);
            fileNames.add(fileName);
        }
        return fileNames;
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

    @Override
    @Transactional
    public void deleteImage(String fileName) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
        Files.deleteIfExists(filePath);
        storageRepository.deleteByName(fileName);
    }

    /*************************************
     * Storage Service Implementations
     *************************************/
    @Override
    @Transactional
    public ResponseEntity<?> serveImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found");
        }

        try {
            byte[] imageData = downloadImage(fileName);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageData);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to load image");
        }
    }

    @Override
    @Transactional
    public List<GetAllImagesResponse> getAll() {
        List<Storage> storages = storageRepository.findAll();

        return storages.stream()
                .map(image -> this.modelMapperService.forResponse().map(image, GetAllImagesResponse.class))
                .collect(Collectors.toList());
    }

    public Resource loadAsResource(String filename) throws MalformedURLException {
        try {
            Path file = Paths.get("src/main/resources/static/uploads").resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            return null;
        } catch (MalformedURLException e) {
            throw e;
        }
    }
}
