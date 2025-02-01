package com.renovatipoint.security.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AuthenticationResponse {
     @JsonProperty("accessToken")
     private String accessToken;
     @JsonProperty("role")
     private String role;
     @JsonProperty("userId")
     private String userId;

     @JsonProperty("userEmail")
     private String userEmail;

//     @JsonProperty("refreshToken")
//     private String refreshToken;

}
