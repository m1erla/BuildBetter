package com.renovatipoint.business.abstracts;

import com.renovatipoint.entities.concretes.Ads;
import com.renovatipoint.entities.concretes.User;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public interface IStorageService {
    String storeFile(MultipartFile file) throws IOException;

    String uploadImage(MultipartFile file, User user) throws IOException;

    List<String> uploadImages(List<MultipartFile> files, User user, Ads ads) throws IOException;

    byte[] downloadImage(String fileName) throws IOException;

    void deleteByName(String fileName);

    ResponseEntity<?> serveImage(String fileName);

    ResponseEntity<?> getImagesByUserId(String userId);

    Resource loadAsResource(String filename) throws MalformedURLException;

    void deleteImage(String fileName);
}