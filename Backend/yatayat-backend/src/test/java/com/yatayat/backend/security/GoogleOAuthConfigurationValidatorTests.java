package com.yatayat.backend.security;

import com.yatayat.backend.config.GoogleOAuthConfigurationValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleOAuthConfigurationValidatorTests {
    @Test
    void acceptsPresentScalarCredentialsAndSpringDefaultCallback() {
        GoogleOAuthConfigurationValidator validator = validator("client-id", "scalar-secret",
                "{baseUrl}/{action}/oauth2/code/{registrationId}");
        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankOrDownloadedJsonCredentialsWithoutPrintingTheirValues() {
        assertThatThrownBy(() -> validator("client-id", " ", callback()).run(args()))
                .hasMessage("Google OAuth client secret is missing or blank.");
        assertThatThrownBy(() -> validator("client-id",
                "{\"web\":{\"client_secret\":\"sensitive\"}}", callback()).run(args()))
                .hasMessageContaining("scalar credential value")
                .hasMessageNotContaining("sensitive");
    }

    @Test
    void rejectsUnexpectedRedirectTemplate() {
        assertThatThrownBy(() -> validator("client-id", "scalar-secret",
                "https://unexpected.example/callback").run(args()))
                .hasMessageContaining("http://localhost:8080/login/oauth2/code/google");
    }

    private GoogleOAuthConfigurationValidator validator(
            String clientId, String secret, String redirectUri) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(secret)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
        ClientRegistrationRepository repository =
                registrationId -> "google".equals(registrationId) ? registration : null;
        return new GoogleOAuthConfigurationValidator(repository);
    }

    private String callback() {
        return "{baseUrl}/{action}/oauth2/code/{registrationId}";
    }

    private DefaultApplicationArguments args() {
        return new DefaultApplicationArguments();
    }
}
