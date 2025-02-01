package com.renovatipoint.business.requests;

import com.renovatipoint.entities.concretes.Role;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateUserRequest {
    private String name;
    private String surname;
    private String email;
    private String password;
    private String phoneNumber;
    private String postCode;
    private Role role;

}
