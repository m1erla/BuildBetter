package com.buildbetter.business.abstracts;

import com.buildbetter.business.responses.GetAllImagesResponse;
import com.buildbetter.entities.concretes.Ads;
import com.buildbetter.entities.concretes.Storage;
import com.buildbetter.entities.concretes.User;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public interface StorageService {

    List<GetAllImagesResponse> getAll();
    String storeFile(MultipartFile file) throws IOException;

    Storage uploadImage(MultipartFile file, User user) throws IOException;

    List<Storage> uploadImages(List<MultipartFile> files, User user, Ads ads) throws IOException;

    // loadAsResource (StorageService'den @Override eklenmeli)
    Resource loadAsResource(String filename) throws MalformedURLException;

    byte[] downloadImage(String fileName) throws IOException;

    void deleteImage(String fileName) throws IOException;

    ResponseEntity<?> serveImage(String fileName);
}
