package com.renovatipoint.business.responses;

import com.renovatipoint.entities.concretes.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {

    private String id;
    private String name;
    private String email;
    private String phoneNumber;

    public static UserDTO fromEntity(User user){
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
