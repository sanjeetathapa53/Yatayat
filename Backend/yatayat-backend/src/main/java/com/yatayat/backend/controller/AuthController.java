package com.yatayat.backend.controller;

import com.yatayat.backend.dto.ChangePasswordRequest;
import com.yatayat.backend.dto.LoginRequest;
import com.yatayat.backend.dto.OtpVerifyRequest;
import com.yatayat.backend.dto.RegisterRequest;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.EmailService;
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
import com.yatayat.backend.dto.ForgotPasswordRequest;
import com.yatayat.backend.entity.Wallet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SessionLogoutService sessionLogoutService;

    private final Map<String, String> otpStorage = new HashMap<>();

    public AuthController(UserRepository userRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder,
                          SessionLogoutService sessionLogoutService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
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

        if (
                request.getEmail() == null ||
                        request.getEmail().isBlank()
        ) {
            return "Email is required";
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return "Email already registered";
        }

        String otp = generateOtp();

        otpStorage.put(email, otp);
        emailService.sendOtpEmail(email, otp);

        return "OTP sent to email";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestBody OtpVerifyRequest request
    ) {
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        String storedOtp = otpStorage.get(email);

        if (storedOtp == null) {
            return "OTP not found";
        }

        if (!storedOtp.equals(request.getOtp())) {
            return "Invalid OTP";
        }

        otpStorage.remove(email);

        return "OTP verified";
    }

    @PostMapping("/register")
    public String register(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {

        if (
                request.getEmail() == null ||
                        request.getEmail().isBlank()
        ) {
            return "Email is required";
        }

        String normalizedEmail =
                request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return "Email already registered";
        }

        String role = request.getRole() == null
                ? ""
                : request.getRole().trim().toUpperCase();

        if (
                !role.equals("PASSENGER") &&
                        !role.equals("DRIVER") &&
                        !role.equals("OPERATOR")
        ) {
            return "Invalid account role";
        }

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
        httpRequest.getSession(true).setAttribute(
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
        httpRequest.getSession(true).setAttribute(
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

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return "Email not registered";
        }

        String otp = generateOtp();
        otpStorage.put(request.getEmail(), otp);

        emailService.sendOtpEmail(request.getEmail(), otp);

        return "OTP sent to email";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody ForgotPasswordRequest request) {

        String storedOtp = otpStorage.get(request.getEmail());

        if (storedOtp == null) {
            return "OTP not found";
        }

        if (!storedOtp.equals(request.getOtp())) {
            return "Invalid OTP";
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return "Email not registered";
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpStorage.remove(request.getEmail());

        return "Password reset successfully";
    }

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}
