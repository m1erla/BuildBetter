package com.buildbetter.business.abstracts;

import com.buildbetter.business.responses.GetAllImagesResponse;
import com.buildbetter.entities.concretes.Ads;
import com.buildbetter.entities.concretes.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StorageService {

    List<GetAllImagesResponse> getAll();
    String storeFile(MultipartFile file) throws IOException;

    String uploadImage(MultipartFile file, User user) throws IOException;

    List<String> uploadImages(List<MultipartFile> files, User user, Ads ads) throws IOException;

    byte[] downloadImage(String fileName) throws IOException;

    void deleteImage(String fileName) throws IOException;

    ResponseEntity<?> serveImage(String fileName);
}
