package com.yatayat.backend.service;

import com.yatayat.backend.dto.ChangePasswordRequest;
import com.yatayat.backend.entity.AuthenticationProvider;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordChangeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(
            String authenticatedEmail,
            ChangePasswordRequest request
    ) {
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Your session is no longer valid. Please log in again."
                ));

        if (user.getAuthenticationProvider() == AuthenticationProvider.GOOGLE
                || user.getPassword() == null
                || user.getPassword().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password changes are not available for Google Sign-In accounts."
            );
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password and confirmation do not match."
            );
        }

        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPassword()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect."
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}