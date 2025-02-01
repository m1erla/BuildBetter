package com.renovatipoint.business.requests;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateAdsRequest {
    private String userId;
    private String title;
    private boolean isActive;
    private String descriptions;
    private List<MultipartFile> storages;
    private String adReleaseDate;
    private String categoryId;
    private String serviceId;
}
