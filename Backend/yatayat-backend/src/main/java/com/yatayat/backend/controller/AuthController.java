package com.yatayat.backend.controller;

import com.yatayat.backend.dto.ChangePasswordRequest;
import com.yatayat.backend.dto.LoginRequest;
import com.yatayat.backend.dto.OtpVerifyRequest;
import com.yatayat.backend.dto.RegisterRequest;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.entity.OtpPurpose;
import com.yatayat.backend.entity.AuthenticationProvider;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.OtpVerificationService;
import com.yatayat.backend.service.SessionLogoutService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.yatayat.backend.dto.ForgotPasswordRequest;
import com.yatayat.backend.entity.Wallet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final OtpVerificationService otpVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final SessionLogoutService sessionLogoutService;

    public AuthController(UserRepository userRepository,
                          OtpVerificationService otpVerificationService,
                          PasswordEncoder passwordEncoder,
                          SessionLogoutService sessionLogoutService) {
        this.userRepository = userRepository;
        this.otpVerificationService = otpVerificationService;
        this.passwordEncoder = passwordEncoder;
        this.sessionLogoutService = sessionLogoutService;
    }

    @GetMapping("/google-login")
    public void googleLogin(HttpSession session, HttpServletResponse response) throws IOException {
        session.setAttribute("googleMode", "LOGIN");
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/google-register")
    public void googleRegister(HttpSession session, HttpServletResponse response) throws IOException {
        session.setAttribute("googleMode", "REGISTER");
        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping("/send-otp")
    public String sendOtp(@RequestBody RegisterRequest request) {
        String email = otpVerificationService.normalizeEmail(
                request == null ? null : request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email is already registered.");
        }
        otpVerificationService.issue(email, OtpPurpose.REGISTRATION);
        return "OTP sent to email";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestBody OtpVerifyRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Email and OTP are required.");
        }
        otpVerificationService.verify(
                request.getEmail(), request.getOtp(), OtpPurpose.REGISTRATION);
        return "OTP verified";
    }

    @PostMapping("/register")
    @Transactional
    public String register(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration details are required.");
        }
        String normalizedEmail = otpVerificationService.normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email is already registered.");
        }

        String role = request.getRole() == null
                ? ""
                : request.getRole().trim().toUpperCase();

        if (
                !role.equals("PASSENGER") &&
                        !role.equals("DRIVER") &&
                        !role.equals("OPERATOR")
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid account role.");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Full name is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least 6 characters.");
        }

        otpVerificationService.consumeVerifiedRegistration(normalizedEmail);
        User user = new User(
                request.getFullName().trim(),
                normalizedEmail,
                request.getPhone(),
                passwordEncoder.encode(request.getPassword()),
                role
        );

        /*
         * Only passengers currently need the ticket wallet.
         * Driver and operator accounts use operational dashboards.
         */
        if (role.equals("PASSENGER")) {
            Wallet wallet = new Wallet(user);
            user.setWallet(wallet);
        }

        userRepository.save(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + user.getRole().toUpperCase()
                                )
                        )
                );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        HttpSession registrationSession = httpRequest.getSession(false);
        if (registrationSession == null) {
            registrationSession = httpRequest.getSession(true);
        } else {
            httpRequest.changeSessionId();
        }
        registrationSession.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        return "Successfully registered";
    }

    @PostMapping("/login")
    public Object login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) return "User not found";
        if (user.getAuthenticationProvider() == AuthenticationProvider.GOOGLE) {
            return "This account uses Google Sign-In. Please continue with Google.";
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return "Password missing. Please register again.";
        }

        try {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return "Invalid password";
            }
        } catch (Exception e) {
            return "Password is not encrypted. Please register again.";
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + user.getRole().toUpperCase()
                                )
                        )
                );

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            session = httpRequest.getSession(true);
        } else {
            httpRequest.changeSessionId();
        }
        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("role", user.getRole());

        return response;
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("role", user.getRole());
        return response;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        sessionLogoutService.logout(request, response);
        return Map.of(
                "success", true,
                "message", "Logged out successfully"
        );
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        User user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);

        if (user == null) return "User not found";
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return "Password missing. Please register again.";
        }

        try {
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                return "Old password is incorrect";
            }
        } catch (Exception e) {
            return "Password is not encrypted. Please register again.";
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password changed successfully";
    }
    @PostMapping("/send-forgot-password-otp")
    public String sendForgotPasswordOtp(@RequestBody ForgotPasswordRequest request) {
        String email = otpVerificationService.normalizeEmail(
                request == null ? null : request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            try {
                otpVerificationService.issue(email, OtpPurpose.PASSWORD_RESET);
            } catch (ResponseStatusException exception) {
                if (exception.getStatusCode().value() != 429) throw exception;
            }
        }
        return "If an account exists for this email, reset instructions have been sent.";
    }

    @PostMapping("/reset-password")
    @Transactional
    public String resetPassword(@RequestBody ForgotPasswordRequest request) {
        if (request == null || request.getNewPassword() == null
                || request.getNewPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A new password of at least 6 characters is required.");
        }
        String email = otpVerificationService.normalizeEmail(request.getEmail());
        otpVerificationService.verifyAndConsume(
                email, request.getOtp(), OtpPurpose.PASSWORD_RESET);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Password reset could not be completed."));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password reset successfully";
    }
}
