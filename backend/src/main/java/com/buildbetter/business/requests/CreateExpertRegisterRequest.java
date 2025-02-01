package com.renovatipoint.business.requests;

import com.renovatipoint.entities.concretes.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateExpertRegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String surname;
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Job title ID is required")
    private String jobTitleId;

    private String jobTitleName;

    @NotBlank(message = "Post code is required")
    private String postCode;
    private String phoneNumber;
    private String address;
    private Role role;
}
