package com.yatayat.backend.controller;

import com.yatayat.backend.dto.AdminLoginRequest;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        Map<String, Object> response = new HashMap<>();

        if (
                request.getEmail() == null ||
                        request.getEmail().isBlank() ||
                        request.getPassword() == null ||
                        request.getPassword().isBlank()
        ) {
            response.put("success", false);
            response.put("message", "Email and password are required");

            return ResponseEntity.badRequest().body(response);
        }

        User admin = userRepository
                .findByEmail(request.getEmail().trim())
                .orElse(null);

        if (
                admin == null ||
                        !"ADMIN".equalsIgnoreCase(admin.getRole())
        ) {
            response.put("success", false);
            response.put("message", "Invalid admin credentials");

            return ResponseEntity.status(401).body(response);
        }

        if (
                !passwordEncoder.matches(
                        request.getPassword(),
                        admin.getPassword()
                )
        ) {
            response.put("success", false);
            response.put("message", "Invalid admin credentials");

            return ResponseEntity.status(401).body(response);
        }

        Map<String, Object> cleanAdmin = new HashMap<>();
        cleanAdmin.put("id", admin.getId());
        cleanAdmin.put("fullName", admin.getFullName());
        cleanAdmin.put("email", admin.getEmail());
        cleanAdmin.put("role", admin.getRole());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        admin.getEmail(),
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        httpRequest.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        response.put("success", true);
        response.put("message", "Admin login successful");
        response.put("admin", cleanAdmin);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
