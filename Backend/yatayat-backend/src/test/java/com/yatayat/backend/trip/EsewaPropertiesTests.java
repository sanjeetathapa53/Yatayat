package com.yatayat.backend.trip;

import com.yatayat.backend.payment.EsewaProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EsewaPropertiesTests {
    @Test
    void disabledConfigurationUsesOfficialSafeDefaults() {
        EsewaProperties properties = new EsewaProperties();
        properties.validate();
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getFormUrl()).isEqualTo(
                "https://rc-epay.esewa.com.np/api/epay/main/v2/form");
        assertThat(properties.getStatusUrl()).isEqualTo(
                "https://rc.esewa.com.np/api/epay/transaction/status/");
    }

    @Test
    void enabledConfigurationRejectsMissingValuesWithoutRenderingSecret() {
        EsewaProperties properties = new EsewaProperties();
        properties.setEnabled(true);
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("product code")
                .hasMessageContaining("secret key")
                .hasMessageNotContaining("Authorization");
    }

    @Test
    void acceptsLocalDevelopmentAndProductionHttpsFrontendUrls() {
        assertThatCode(enabled("http://localhost:5173")::validate).doesNotThrowAnyException();
        assertThatCode(enabled("http://127.0.0.1:5173")::validate).doesNotThrowAnyException();
        assertThatCode(enabled("https://yatayat.example.com")::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsafeFrontendAndNonOfficialProviderUrls() {
        assertThatThrownBy(enabled("javascript:alert(1)")::validate)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(enabled("http://yatayat.example.com")::validate)
                .isInstanceOf(IllegalStateException.class);
        EsewaProperties wrongProvider = enabled("http://localhost:5173");
        wrongProvider.setFormUrl("https://example.com/checkout");
        assertThatThrownBy(wrongProvider::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void statusUrlRequiresExactOfficialHttpsSandboxHostWithoutUserInfo() {
        EsewaProperties valid = enabled("http://localhost:5173");
        valid.setStatusUrl("https://rc.esewa.com.np/api/epay/transaction/status/");
        assertThatCode(valid::validate).doesNotThrowAnyException();

        EsewaProperties oldHost = enabled("http://localhost:5173");
        oldHost.setStatusUrl("https://uat.esewa.com.np/api/epay/transaction/status/");
        assertThatThrownBy(oldHost::validate).isInstanceOf(IllegalStateException.class);

        EsewaProperties insecure = enabled("http://localhost:5173");
        insecure.setStatusUrl("http://rc.esewa.com.np/api/epay/transaction/status/");
        assertThatThrownBy(insecure::validate).isInstanceOf(IllegalStateException.class);

        EsewaProperties arbitraryHost = enabled("http://localhost:5173");
        arbitraryHost.setStatusUrl("https://example.com/api/epay/transaction/status/");
        assertThatThrownBy(arbitraryHost::validate).isInstanceOf(IllegalStateException.class);

        EsewaProperties userInfo = enabled("http://localhost:5173");
        userInfo.setStatusUrl("https://user@rc.esewa.com.np/api/epay/transaction/status/");
        assertThatThrownBy(userInfo::validate).isInstanceOf(IllegalStateException.class);
    }

    private EsewaProperties enabled(String frontendUrl) {
        EsewaProperties properties = new EsewaProperties();
        properties.setEnabled(true);
        properties.setProductCode("EPAYTEST");
        properties.setSecretKey("unit-test-secret");
        properties.setFrontendBaseUrl(frontendUrl);
        return properties;
    }
}
