package com.yatayat.backend.security;

import com.yatayat.backend.config.AuthenticationProviderMigration;
import com.yatayat.backend.entity.AuthenticationProvider;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AuthenticationProviderMigrationTests {
    @Test
    void existingPasswordAndGoogleUsersAreClassifiedWithoutDataLoss() {
        User local = new User("Local", "local@example.com", "9800000000",
                "encoded-password", "PASSENGER");
        local.setAuthenticationProvider(null);
        User google = new User("Google", "google@example.com", "", null, "PASSENGER");

        UserRepository users = mock(UserRepository.class);
        when(users.findAll()).thenReturn(List.of(local, google));

        new AuthenticationProviderMigration(users).run(new DefaultApplicationArguments());

        assertThat(local.getAuthenticationProvider()).isEqualTo(AuthenticationProvider.LOCAL);
        assertThat(google.getAuthenticationProvider()).isEqualTo(AuthenticationProvider.GOOGLE);
        assertThat(local.getPassword()).isEqualTo("encoded-password");
        assertThat(google.getPassword()).isNull();
        verify(users).saveAll(anyList());
    }
}
