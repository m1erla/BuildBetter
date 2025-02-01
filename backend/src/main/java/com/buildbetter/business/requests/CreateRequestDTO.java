package com.renovatipoint.business.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRequestDTO {

    @NotBlank(message = "Ad ID is required")
    private String adId;

    private String expertEmail;
    private String expertName;
    private String expertId;
    private String userId;

    private String message;

}
