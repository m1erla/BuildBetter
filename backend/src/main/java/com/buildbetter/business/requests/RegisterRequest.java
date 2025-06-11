package com.buildbetter.business.requests;

import com.buildbetter.entities.concretes.Role;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String surname;
    private String password;
    private String email;
    private String phoneNumber;
    private String postCode;
    private Role role;
}
