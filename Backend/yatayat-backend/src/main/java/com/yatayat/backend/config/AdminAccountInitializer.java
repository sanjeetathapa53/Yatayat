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
        User existingUser =
                userRepository.findByEmail(adminEmail).orElse(null);

        if (existingUser != null) {
            if (!"ADMIN".equalsIgnoreCase(existingUser.getRole())) {
                existingUser.setRole("ADMIN");
                userRepository.save(existingUser);
            }

            System.out.println(
                    "Yatayat admin account already exists: " + adminEmail
            );

            return;
        }

        User admin = new User(
                adminName,
                adminEmail,
                "",
                passwordEncoder.encode(adminPassword),
                "ADMIN"
        );

        userRepository.save(admin);

        System.out.println(
                "Yatayat admin account created: " + adminEmail
        );
    }
}