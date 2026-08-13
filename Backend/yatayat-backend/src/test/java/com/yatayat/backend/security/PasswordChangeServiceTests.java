package com.yatayat.backend.security;

import com.yatayat.backend.dto.ChangePasswordRequest;
import com.yatayat.backend.entity.AuthenticationProvider;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.PasswordChangeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordChangeService service;
    private User driver;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeService(userRepository, passwordEncoder);
        driver = new User(
                "Driver",
                "driver@example.com",
                "9800000000",
                "stored-hash",
                "DRIVER"
        );
        driver.setAuthenticationProvider(AuthenticationProvider.LOCAL);
    }

    @Test
    void verifiesCurrentPasswordHashesAndPersistsNewPassword() {
        ChangePasswordRequest request =
                request("current-password", "new-password", "new-password");
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driver));
        when(passwordEncoder.matches("current-password", "stored-hash"))
                .thenReturn(true);
        when(passwordEncoder.encode("new-password"))
                .thenReturn("new-stored-hash");

        service.changePassword("driver@example.com", request);

        assertThat(driver.getPassword()).isEqualTo("new-stored-hash");
        assertThat(driver.getPassword()).isNotEqualTo("new-password");
        verify(userRepository).save(driver);
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutPersisting() {
        ChangePasswordRequest request =
                request("wrong-password", "new-password", "new-password");
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driver));
        when(passwordEncoder.matches("wrong-password", "stored-hash"))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.changePassword("driver@example.com", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(driver);
        verify(passwordEncoder, never()).encode("new-password");
    }

    @Test
    void rejectsConfirmationMismatchWithoutCheckingOrPersisting() {
        ChangePasswordRequest request =
                request("current-password", "new-password", "different");
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driver));

        assertThatThrownBy(() ->
                service.changePassword("driver@example.com", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("do not match");

        verify(passwordEncoder, never()).matches(
                "current-password",
                "stored-hash"
        );
        verify(userRepository, never()).save(driver);
    }

    @Test
    void rejectsGooglePasswordlessAccount() {
        driver.setAuthenticationProvider(AuthenticationProvider.GOOGLE);
        driver.setPassword(null);
        ChangePasswordRequest request =
                request("current-password", "new-password", "new-password");
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driver));

        assertThatThrownBy(() ->
                service.changePassword("driver@example.com", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Google Sign-In");

        verify(userRepository, never()).save(driver);
    }

    @Test
    void resolvesOnlyTheAuthenticatedEmail() {
        ChangePasswordRequest request =
                request("current-password", "new-password", "new-password");
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driver));
        when(passwordEncoder.matches("current-password", "stored-hash"))
                .thenReturn(true);
        when(passwordEncoder.encode("new-password"))
                .thenReturn("new-stored-hash");

        service.changePassword("driver@example.com", request);

        verify(userRepository).findByEmailIgnoreCase("driver@example.com");
        verify(userRepository, never()).findByEmailIgnoreCase("other@example.com");
    }

    private ChangePasswordRequest request(
            String currentPassword,
            String newPassword,
            String confirmation
    ) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(currentPassword);
        request.setNewPassword(newPassword);
        request.setConfirmNewPassword(confirmation);
        return request;
    }
}