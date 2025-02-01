package com.renovatipoint.business.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChangePasswordRequest {

    private String password;
    private String newPassword;
    private String confirmationPassword;
}