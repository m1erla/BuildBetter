package com.renovatipoint.business.requests;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateAdsRequest {
    private String id;
    private String userId;
    private String title;
    private boolean isActive;
    private String descriptions;
    private List<MultipartFile> storages;
    private String adReleaseDate;
    private String categoryName;
    private String serviceName;
    private String categoryId;
    private String serviceId;
}
