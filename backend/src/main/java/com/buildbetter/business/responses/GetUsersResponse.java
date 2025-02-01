package com.renovatipoint.business.responses;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GetUsersResponse {
    private String id;
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private String profileImage;
    private String address;
    private List<GetAllImagesResponse> storages;
    private String postCode;
    private String role;
    private List<GetAllAdsResponse> ads;
}
