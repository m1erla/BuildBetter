package com.renovatipoint.security.auth;

import com.renovatipoint.business.requests.CreateExpertRegisterRequest;
import com.renovatipoint.business.requests.RegisterRequest;
import com.renovatipoint.business.responses.ExpertRegisterResponse;
import com.renovatipoint.business.responses.RegisterResponse;
import com.renovatipoint.business.rules.UserBusinessRules;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final UserBusinessRules userBusinessRules;

    //        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//        System.out.println("The secret key is : " + key);
//        System.out.println(Base64.getEncoder().encodeToString(key.getEncoded()));

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody RegisterRequest request
    ){
         try {
    return ResponseEntity.ok(service.register(request));
         }catch (IllegalStateException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RegisterResponse(ex.getMessage(), "", request.getEmail()));
         }
    }

    @PostMapping("/expertRegister")
    public ResponseEntity<ExpertRegisterResponse> registerExpert(@RequestBody CreateExpertRegisterRequest request){
        try {
            return ResponseEntity.ok(service.expertRegister(request));
        }catch (IllegalStateException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExpertRegisterResponse(ex.getMessage(), "", request.getEmail(), request.getName()));
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ){
        if(!userBusinessRules.userExists(request.getEmail())){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthenticationResponse());
        }
    try {
    return ResponseEntity.ok(service.authenticate(request));
         }catch (IllegalArgumentException ex){
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthenticationResponse());
          }

    }

    @PostMapping("/refresh-token")
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    )throws IOException {
        service.refreshToken(request, response);
    }

}

//    @PostMapping("/expertRegister")
//    public ResponseEntity<?> registerExpert(@Valid @RequestBody ExpertRegisterRequest request, BindingResult bindingResult) {
//        if (bindingResult.hasErrors()) {
//            List<String> errors = bindingResult.getAllErrors().stream()
//                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
//                    .collect(Collectors.toList());
//            return ResponseEntity.badRequest().body(errors);
//        }
//
//        try {
//            ExpertRegisterResponse response = service.expertRegister(request);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }