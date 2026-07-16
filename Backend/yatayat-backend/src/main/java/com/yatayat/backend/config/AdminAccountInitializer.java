package com.yatayat.backend.config;

import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${yatayat.admin.email}")
    private String adminEmail;

    @Value("${yatayat.admin.password}")
    private String adminPassword;

    @Value("${yatayat.admin.name}")
    private String adminName;

    public AdminAccountInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        validateConfiguration();

        String normalizedEmail =
                adminEmail.trim().toLowerCase();

        User existingUser =
                userRepository
                        .findByEmailIgnoreCase(normalizedEmail)
                        .orElse(null);

        if (existingUser != null) {
            synchronizeExistingAdmin(
                    existingUser,
                    normalizedEmail
            );
            return;
        }

        createAdmin(normalizedEmail);
    }

    private void synchronizeExistingAdmin(
            User existingUser,
            String normalizedEmail
    ) {
        boolean changed = false;

        if (!"ADMIN".equalsIgnoreCase(existingUser.getRole())) {
            existingUser.setRole("ADMIN");
            changed = true;
        }

        if (
                existingUser.getPassword() == null ||
                        existingUser.getPassword().isBlank() ||
                        !passwordEncoder.matches(
                                adminPassword,
                                existingUser.getPassword()
                        )
        ) {
            existingUser.setPassword(
                    passwordEncoder.encode(adminPassword)
            );
            changed = true;
        }

        if (
                existingUser.getFullName() == null ||
                        existingUser.getFullName().isBlank() ||
                        !existingUser.getFullName()
                                .equals(adminName.trim())
        ) {
            existingUser.setFullName(adminName.trim());
            changed = true;
        }

        if (
                existingUser.getEmail() == null ||
                        !existingUser.getEmail()
                                .equals(normalizedEmail)
        ) {
            existingUser.setEmail(normalizedEmail);
            changed = true;
        }

        if (changed) {
            userRepository.save(existingUser);

            System.out.println(
                    "Yatayat admin account synchronized: "
                            + normalizedEmail
            );
        } else {
            System.out.println(
                    "Yatayat admin account already up to date: "
                            + normalizedEmail
            );
        }
    }

    private void createAdmin(String normalizedEmail) {
        User admin = new User(
                adminName.trim(),
                normalizedEmail,
                "",
                passwordEncoder.encode(adminPassword),
                "ADMIN"
        );

        userRepository.save(admin);

        System.out.println(
                "Yatayat admin account created: "
                        + normalizedEmail
        );
    }

    private void validateConfiguration() {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "yatayat.admin.email is required"
            );
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "yatayat.admin.password is required"
            );
        }

        if (adminName == null || adminName.isBlank()) {
            throw new IllegalStateException(
                    "yatayat.admin.name is required"
            );
        }
    }
}