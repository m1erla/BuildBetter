//package com.buildbetter.webApi.controllers;
//
//import com.buildbetter.entities.concretes.Role;
//import com.buildbetter.entities.concretes.User;
//import com.buildbetter.dataAccess.abstracts.UserRepository;
//import com.buildbetter.enums.Status;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.math.BigDecimal;
//
///**
// * Bu sınıf, uygulama başladığında default bir admin kullanıcısı oluşturmak için
// * kullanılır.
// * Güvenlik sebebiyle production ortamında kullanılırken dikkatli olunmalıdır.
// * İlk kullanımdan sonra bu sınıfı devre dışı bırakabilir veya
// * kaldırabilirsiniz.
// */
//@Configuration
//public class CreateAdminScript {
//
//    @Bean
//    public CommandLineRunner createAdminUser(
//            UserRepository userRepository,
//            PasswordEncoder passwordEncoder) {
//
//        return args -> {
//            // Admin kullanıcısının e-posta adresi ile kontrol et
//            String adminEmail = "admin@renovatipoint.com";
//            if (userRepository.findByEmail(adminEmail).isEmpty()) {
//                User adminUser = User.builder()
//                        .name("Admin")
//                        .surname("User")
//                        .email(adminEmail)
//                        .phoneNumber("+905551234567")
//                        .password(passwordEncoder.encode("Admin123!"))
//                        .role(Role.ADMIN)
//                        .status(Status.ONLINE)
//                        .accountBlocked(false)
//                        .balance(BigDecimal.ZERO)
//                        .paymentIssuesCount(0)
//                        .build();
//
//                userRepository.save(adminUser);
//                System.out.println("ADMIN user created successfully with email: " + adminEmail);
//            } else {
//                System.out.println("ADMIN user already exists with email: " + adminEmail);
//            }
//        };
//    }
//}