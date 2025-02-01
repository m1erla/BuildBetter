package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.concretes.StorageManager;
import com.renovatipoint.business.responses.GetAllImagesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@CrossOrigin
public class StorageController {
    private final Path uploadPath = Paths.get("src/main/resources/static/uploads");
    private final StorageManager fileStorageService;

    @Autowired
    public StorageController(StorageManager fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<GetAllImagesResponse>> getAllImages() {
        List<GetAllImagesResponse> images = fileStorageService.getAll();
        return ResponseEntity.ok(images);
    }

    @GetMapping(value = "/{fileName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            Path file = uploadPath.resolve(fileName);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; fileName=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
