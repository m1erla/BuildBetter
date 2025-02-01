package com.renovatipoint.business.requests;

import com.renovatipoint.entities.concretes.Role;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class UpdateUserRequest {
    private String id;
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private String address;
    private MultipartFile storages;
    private String postCode;
    private Role role;

    public UpdateUserRequest(String id, String name, String surname, String email, String phoneNumber, String address, MultipartFile storages, String postCode, Role role) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.storages = storages;
        this.postCode = postCode;
        this.role = role;
    }

    public UpdateUserRequest(){}

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setStorages(MultipartFile storages) {
        this.storages = storages;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public MultipartFile getStorages() {
        return storages;
    }

    public String getPostCode() {
        return postCode;
    }


    public Role getRole() {
        return role;
    }
}
