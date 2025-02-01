package com.renovatipoint.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renovatipoint.business.concretes.StripeManager;
import com.renovatipoint.business.requests.CreateExpertRegisterRequest;
import com.renovatipoint.business.requests.RegisterRequest;
import com.renovatipoint.business.responses.ExpertRegisterResponse;
import com.renovatipoint.business.responses.RegisterResponse;
import com.renovatipoint.core.utilities.mappers.ModelMapperService;
import com.renovatipoint.dataAccess.abstracts.ExpertRepository;
import com.renovatipoint.dataAccess.abstracts.JobTitleRepository;
import com.renovatipoint.entities.concretes.*;
import com.renovatipoint.enums.Status;
import com.renovatipoint.security.jwt.JwtService;
import com.renovatipoint.dataAccess.abstracts.UserRepository;
import com.renovatipoint.security.token.*;
import com.stripe.exception.StripeException;
import com.stripe.model.SetupIntent;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final ExpertRepository expertRepository;
    private final TokenRepository tokenRepository;
    private final JobTitleRepository jobTitleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapperService modelMapperService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final StripeManager stripeManager;

    private final static Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public ExpertRegisterResponse expertRegister(CreateExpertRegisterRequest request) throws StripeException {
        String stripeCustomerId = null;
        try {
            if (repository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalStateException("Expert with this email already exists!");
            }

            // Validate and get job title - Add explicit logging
            logger.info("Looking up job title with ID: {}", request.getJobTitleId());
            JobTitle jobTitle = jobTitleRepository.findById(request.getJobTitleId())
                    .orElseThrow(() -> {
                        logger.error("Job title not found with ID: {}", request.getJobTitleId());
                        return new EntityNotFoundException("Job title not found!");
                    });
            logger.info("Found job title: {}", jobTitle.getName());

            // Create Stripe customer
            stripeCustomerId = stripeManager.createStripeCustomer(request.getEmail(), request.getName());
            logger.info("Created Stripe customer with ID: {}", stripeCustomerId);

            Expert experts = new Expert();

            experts.setName(request.getName());
            experts.setSurname(request.getSurname());
            experts.setPhoneNumber(request.getPhoneNumber());
            experts.setPostCode(request.getPostCode());
            experts.setJobTitle(jobTitle);
            experts.setPassword(passwordEncoder.encode(request.getPassword()));
            experts.setEmail(request.getEmail());
            experts.setStripeCustomerId(stripeCustomerId);
            experts.setStatus(Status.ONLINE);
            experts.setBalance(BigDecimal.ZERO);
            experts.setRole(Role.EXPERT);
            experts.setAccountBlocked(false);
            experts.setPaymentIssuesCount(0);


            // Address null kontrolü ekleyelim
            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                throw new IllegalArgumentException("Address cannot be null or empty");
            }
            experts.setAddress(request.getAddress().trim());

            // Expert'i kaydet
            logger.info("Attempting to save expert with Stripe customer ID: {}", stripeCustomerId);
            Expert savedExpert = expertRepository.save(experts);

            // Kaydedilen expert'in Stripe customer ID'sini kontrol et
            if (savedExpert.getStripeCustomerId() == null
                    || !savedExpert.getStripeCustomerId().equals(stripeCustomerId)) {
                logger.error("Stripe customer ID was not properly saved. Expected: {}, Actual: {}",
                        stripeCustomerId, savedExpert.getStripeCustomerId());
                throw new IllegalStateException("Failed to save Stripe customer ID");
            }
            logger.info("Expert saved successfully with Stripe customer ID: {}", savedExpert.getStripeCustomerId());

            // Validate if the expert ID is set after saving
            if (savedExpert.getId() == null) {
                throw new IllegalStateException(
                        "Expert ID is invalid after saving. Please verify expert registration.");
            }

            String jwtToken = jwtService.generateToken(savedExpert);
            saveUserToken(savedExpert, jwtToken);

            SetupIntent setupIntent = stripeManager.createSepaSetupIntent(stripeCustomerId);

            return ExpertRegisterResponse.builder()
                    .message("Expert Registered Successfully.")
                    .expertId(savedExpert.getId())
                    .email(savedExpert.getEmail())
                    .paymentMethodId(setupIntent.getClientSecret())
                    .build();
        } catch (DataIntegrityViolationException e) {
            if (stripeCustomerId != null) {
                stripeManager.deleteStripeCustomer(stripeCustomerId);
            }
            logger.error("Data integrity violation during expert registration", e);
            throw new IllegalArgumentException("Invalid data provided for expert registration", e);
        } catch (ConstraintViolationException e) {
            if (stripeCustomerId != null) {
                stripeManager.deleteStripeCustomer(stripeCustomerId);
            }
            logger.error("Constraint violation during expert registration", e);
            String violationMessages = e.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Constraint violations: " + violationMessages, e);
        } catch (Exception e) {
            if (stripeCustomerId != null) {
                stripeManager.deleteStripeCustomer(stripeCustomerId);
            }
            logger.error("Unexpected error during expert registration", e);
            throw new RuntimeException("An unexpected error occurred during expert registration", e);
        }
    }

    public RegisterResponse register(RegisterRequest request) {

        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("User with this email already exists!");
        }
        if (repository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalStateException(
                    "This phone number is already in use. Please try a different phone number!");
        }
        var user = User.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .postCode(request.getPostCode())
                .role(Role.USER)
                .accountBlocked(false)
                .balance(BigDecimal.ZERO)
                .status(Status.ONLINE)
                .paymentIssuesCount(0)
                .build();
        var savedUser = repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(savedUser, jwtToken);

        return RegisterResponse.builder()
                .message("User Created Successfully!")
                .userId(user.getId())
                .email(user.getEmail())
                .build();

    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            var user = repository.findByEmail(request.getEmail()).orElseThrow(
                    () -> new IllegalArgumentException("Invalid email or password!"));

            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            revokeAllUserTokens(user);
            saveUserToken(user, jwtToken);

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .role(user.getRole().name())
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .build();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid email or password!");
        }
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validConsumerTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validConsumerTokens.isEmpty())
            return;
        validConsumerTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validConsumerTokens);
    }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail).orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

}
