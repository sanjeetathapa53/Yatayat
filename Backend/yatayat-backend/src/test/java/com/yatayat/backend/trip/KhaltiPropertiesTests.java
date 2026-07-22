package com.yatayat.backend.trip;

import com.yatayat.backend.payment.KhaltiProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class KhaltiPropertiesTests {
    @Test
    void disabledConfigurationUsesSafeDefaults() {
        KhaltiProperties properties = new KhaltiProperties();
        properties.validate();
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getInitiateUrl()).startsWith("https://dev.khalti.com/");
        assertThat(properties.getLookupUrl()).startsWith("https://dev.khalti.com/");
    }

    @Test
    void enabledConfigurationRejectsMissingValuesWithoutRenderingSecret() {
        KhaltiProperties properties = new KhaltiProperties();
        properties.setEnabled(true);
        properties.setSecretKey("");
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secret key")
                .hasMessageNotContaining("Authorization")
                .hasMessageNotContaining("Key ");
    }

    @Test
    void enabledConfigurationAcceptsHttpForLocalDevelopmentHosts() {
        KhaltiProperties localhost = enabledProperties("http://localhost:5173");
        KhaltiProperties loopback = enabledProperties("http://127.0.0.1:5173");

        assertThatCode(localhost::validate).doesNotThrowAnyException();
        assertThatCode(loopback::validate).doesNotThrowAnyException();
    }

    @Test
    void enabledConfigurationAcceptsHttpsForProductionHost() {
        assertThatCode(enabledProperties("https://yatayat.example.com")::validate)
                .doesNotThrowAnyException();
    }

    @Test
    void enabledConfigurationRejectsMalformedHostlessAndUnsafeFrontendUrls() {
        assertThatThrownBy(enabledProperties("http://localhost:5173}")::validate)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(enabledProperties("http:/missing-host")::validate)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(enabledProperties("javascript:alert(1)")::validate)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(enabledProperties("file:///tmp/index.html")::validate)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(enabledProperties("data:text/html,test")::validate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void enabledConfigurationRejectsInsecureHttpForNonLocalHost() {
        assertThatThrownBy(enabledProperties("http://yatayat.example.com")::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    private KhaltiProperties enabledProperties(String frontendBaseUrl) {
        KhaltiProperties properties = new KhaltiProperties();
        properties.setEnabled(true);
        properties.setSecretKey("test-secret");
        properties.setFrontendBaseUrl(frontendBaseUrl);
        return properties;
    }
}
