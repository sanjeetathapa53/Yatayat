package com.yatayat.backend.service;

import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public User requireOwnedUser(
            Authentication authentication,
            Long requestedUserId
    ) {
        User user = requireUser(authentication);

        if (requestedUserId == null || !user.getId().equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return user;
    }
}
