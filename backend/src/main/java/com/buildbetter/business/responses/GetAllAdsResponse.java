package com.renovatipoint.business.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllAdsResponse {
    private String id;
    private String title;
    private String descriptions;
    private String imageUrl;
    private String adReleaseDate;
    private String categoryId;
    private String categoryName;
    private String serviceId;
    private String serviceName;
    private String userId;
    private String userName;
    private List<GetAllImagesResponse> storages;
}
