package com.renovatipoint.business.requests;

import com.renovatipoint.entities.concretes.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String surname;
    private String email;
    private String password;
    private String phoneNumber;
    private String postCode;
    private Role role;
}
