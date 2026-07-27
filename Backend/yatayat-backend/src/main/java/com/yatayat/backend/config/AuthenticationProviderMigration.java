package com.yatayat.backend.config;

import com.yatayat.backend.entity.AuthenticationProvider;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthenticationProviderMigration implements ApplicationRunner {
    private final UserRepository users;

    public AuthenticationProviderMigration(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> changed = new ArrayList<>();
        for (User user : users.findAll()) {
            AuthenticationProvider expected =
                    user.getPassword() == null || user.getPassword().isBlank()
                            ? AuthenticationProvider.GOOGLE
                            : AuthenticationProvider.LOCAL;
            if (user.getAuthenticationProvider() != expected) {
                user.setAuthenticationProvider(expected);
                changed.add(user);
            }
        }
        if (!changed.isEmpty()) users.saveAll(changed);
    }
}
