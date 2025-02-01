package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.AdsService;
import com.renovatipoint.business.concretes.AdsManager;
import com.renovatipoint.business.concretes.StorageManager;
import com.renovatipoint.business.requests.CreateAdsRequest;
import com.renovatipoint.business.requests.UpdateAdsRequest;
import com.renovatipoint.business.responses.GetAllAdsResponse;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import com.renovatipoint.entities.concretes.User;
import com.renovatipoint.security.jwt.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/ads")
@AllArgsConstructor
public class AdsController {
    private final AdsManager adsManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AdsService adsService;
    private final StorageManager storageManager;

    @GetMapping
    public List<GetAllAdsResponse> getAllAds() {
        return adsManager.getAll();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GetAllAdsResponse>> getUserAds(@PathVariable String userId) {
        try {
            List<GetAllAdsResponse> userAds = adsManager.getUserAdById(userId);

            return ResponseEntity.ok(userAds);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/user-images")
    public ResponseEntity<?> getUserAdImages(@RequestHeader("Authorization") String authorizationHeader) {
        String jwt = authorizationHeader.substring(7).trim();
        String email = jwtService.extractUsername(jwt);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found!"));
        return adsManager.getAdImagesForUser(user.getId());
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<?> getAdImages(@PathVariable String id) {
        try {
            return adsManager.getAdImages(id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ad not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving ad images");
        }
    }

    @GetMapping("/{id}/image/{imageName}")
    public ResponseEntity<?> getImage(@PathVariable String id, @PathVariable String imageName) {
        try {
            if (!adsManager.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = storageManager.loadAsResource(imageName);
            if (resource == null || !resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = determineContentType(imageName);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            default:
                return "application/octet-stream";
        }
    }

    @GetMapping("/default-image")
    public ResponseEntity<?> getDefaultImage() {
        try {
            int randomId = new Random().nextInt(1000) + 1;
            String picsumUrl = "https://picsum.photos/id/" + randomId + "/300/200";
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(new URI(picsumUrl))
                    .build();
        } catch (Exception e) {
            try {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(new URI("https://picsum.photos/300/200"))
                        .build();
            } catch (URISyntaxException ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @PostMapping("/ads/{id}/uploadAdImage")
    public ResponseEntity<?> uploadAdImage(@PathVariable String id, @RequestParam("file") List<MultipartFile> files) {
        try {
            List<String> message = adsManager.uploadAdImage(id, files);
            return ResponseEntity.ok(message);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Failed to upload ad image");
        }
    }

    @PostMapping("/ads/{id}/updateAdImages")
    public ResponseEntity<?> updateAdImages(@PathVariable String id, @RequestParam("file") List<MultipartFile> files) {
        try {
            String message = adsManager.updateAdImage(id, files);
            return ResponseEntity.ok(message);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Failed to update ad images");
        }
    }

    @PostMapping(value = "/ad", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> add(@ModelAttribute CreateAdsRequest createAdsRequest) {
        return adsManager.add(createAdsRequest);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> update(@PathVariable String id, @ModelAttribute UpdateAdsRequest updateAdsRequest) {
        updateAdsRequest.setId(id);
        updateAdsRequest.setUserId(updateAdsRequest.getUserId());
        return adsManager.update(updateAdsRequest);
    }

    public ResponseEntity<?> updateAdImage(@PathVariable String id, @RequestParam("files") List<MultipartFile> files)
            throws IOException {
        return ResponseEntity.ok(adsManager.updateAdImage(id, files));
    }

    @DeleteMapping("/{id}/ad-image")
    public ResponseEntity<?> deleteAdImage(@PathVariable String id) {
        return adsManager.deleteAdImage(id);
    }

    @DeleteMapping("/{id}")
    public void deleteAd(@PathVariable String id) {
        this.adsManager.delete(id);
    }
}
