package com.renovatipoint.business.abstracts;

import com.renovatipoint.business.requests.UpdateUserRequest;
import com.renovatipoint.business.responses.*;
import com.renovatipoint.entities.concretes.Invoice;
import com.renovatipoint.entities.concretes.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;

public interface UserService {
    List<GetUsersResponse> getAll();

    GetUsersResponse getUserByEmail(String email);
    GetExpertResponse getExpertByEmail(String email);
    User getByEmail(String email);

    GetUsersResponse getById(String userId);

    ResponseEntity<?> update(UpdateUserRequest updateUserRequest);

    ResponseEntity<?> uploadUserProfileImage(MultipartFile file, String id) throws IOException;

    ResponseEntity<?> getUserProfileImage(String id);

    ResponseEntity<?> getImageWithFileName(String email);

    ResponseEntity<?> deleteUserProfileImage(String id);;

    void sendPaymentConfirmationEmail(String userEmail, Invoice invoice);

    void sendPaymentFailureEmail(String userEmail, Invoice invoice);

    void saveUser(User user);

    void disconnect(User user);

    List<User> findConnectedUsers();




}
