package com.renovatipoint.business.responses;

import lombok.*;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GetExpertResponse {
    private String id;
    private String name;
    private String surname;
    private String phoneNumber;
    private String address;
    private String email;
    private String postCode;
    private String profileImage;
    private String jobTitleName;
    private String jobTitleId;
    private List<GetAllImagesResponse> storages;
    private String role;
    private GetPaymentInfoResponse paymentInfo;
}
