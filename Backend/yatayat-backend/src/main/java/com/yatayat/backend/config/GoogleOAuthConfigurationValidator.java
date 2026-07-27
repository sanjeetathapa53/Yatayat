package com.yatayat.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthConfigurationValidator implements ApplicationRunner {
    private static final String DEFAULT_REDIRECT_TEMPLATE =
            "{baseUrl}/{action}/oauth2/code/{registrationId}";
    private static final String EXPLICIT_REDIRECT_TEMPLATE =
            "{baseUrl}/login/oauth2/code/{registrationId}";
    private final ClientRegistrationRepository registrations;

    public GoogleOAuthConfigurationValidator(ClientRegistrationRepository registrations) {
        this.registrations = registrations;
    }

    @Override
    public void run(ApplicationArguments args) {
        ClientRegistration google = registrations.findByRegistrationId("google");
        if (google == null) {
            throw new IllegalStateException("Google OAuth client registration is missing.");
        }
        requireCredential(google.getClientId(), "client ID");
        requireCredential(google.getClientSecret(), "client secret");
        if (!DEFAULT_REDIRECT_TEMPLATE.equals(google.getRedirectUri())
                && !EXPLICIT_REDIRECT_TEMPLATE.equals(google.getRedirectUri())) {
            throw new IllegalStateException(
                    "Google OAuth redirect URI template must resolve to "
                            + "http://localhost:8080/login/oauth2/code/google for local development.");
        }
    }

    private void requireCredential(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Google OAuth " + label + " is missing or blank.");
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            throw new IllegalStateException(
                    "Google OAuth " + label
                            + " must be the scalar credential value, not a downloaded JSON document.");
        }
    }
}
