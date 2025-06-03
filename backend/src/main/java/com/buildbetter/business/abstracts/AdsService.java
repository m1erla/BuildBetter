package com.buildbetter.business.abstracts;

import com.buildbetter.business.requests.CreateAdsRequest;
import com.buildbetter.business.requests.UpdateAdsRequest;
import com.buildbetter.business.responses.GetAllAdsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AdsService {

    List<GetAllAdsResponse> getAll();

    List<GetAllAdsResponse> getUserAdById(String userId);


    GetAllAdsResponse getAdById(String adId);
    ResponseEntity<?> add(CreateAdsRequest createAdsRequest);

    ResponseEntity<?> update(UpdateAdsRequest updateAdsRequest);
    ResponseEntity<?> getAdImagesForUser(String userId);

    List<String> uploadAdImage(String id, List<MultipartFile> files) throws IOException;

    ResponseEntity<?> getAdImages(String id);

    ResponseEntity<?> deleteAdImage(String id);

    String updateAdImage(String id, List<MultipartFile> files) throws IOException;

    void delete(String id);
}
