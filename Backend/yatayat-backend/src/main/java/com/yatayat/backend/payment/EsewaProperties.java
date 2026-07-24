package com.yatayat.backend.payment;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "yatayat.payments.esewa")
public class EsewaProperties {
    private boolean enabled;
    private String productCode = "";
    private String secretKey = "";
    private String formUrl = "https://rc-epay.esewa.com.np/api/epay/main/v2/form";
    private String statusUrl = "https://rc.esewa.com.np/api/epay/transaction/status/";
    private String frontendBaseUrl = "http://localhost:5173";

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        List<String> missing = new ArrayList<>();
        if (blank(productCode)) missing.add("product code");
        if (blank(secretKey)) missing.add("secret key");
        if (blank(formUrl)) missing.add("form URL");
        if (blank(statusUrl)) missing.add("status URL");
        if (blank(frontendBaseUrl)) missing.add("frontend base URL");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("eSewa is enabled but required configuration is missing: "
                    + String.join(", ", missing));
        }
        requireOfficialUrl(formUrl, "form URL", "rc-epay.esewa.com.np");
        requireOfficialUrl(statusUrl, "status URL", "rc.esewa.com.np");
        requireSafeFrontendBaseUrl(frontendBaseUrl);
    }

    private void requireOfficialUrl(String value, String label, String host) {
        URI uri = parse(value, label);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !host.equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null) {
            throw new IllegalStateException("eSewa " + label
                    + " must use the official HTTPS sandbox host.");
        }
    }

    private void requireSafeFrontendBaseUrl(String value) {
        URI uri = parse(value, "frontend base URL");
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || host.isBlank() || uri.getUserInfo() != null) {
            throw new IllegalStateException("eSewa frontend base URL is invalid.");
        }
        if ("https".equalsIgnoreCase(scheme)) return;
        boolean local = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
        if (!"http".equalsIgnoreCase(scheme) || !local) {
            throw new IllegalStateException(
                    "eSewa frontend base URL must use HTTPS, except HTTP localhost is allowed for development.");
        }
    }

    private URI parse(String value, String label) {
        try {
            return URI.create(value.trim());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("eSewa " + label + " is invalid.");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getFormUrl() { return formUrl; }
    public void setFormUrl(String formUrl) { this.formUrl = formUrl; }
    public String getStatusUrl() { return statusUrl; }
    public void setStatusUrl(String statusUrl) { this.statusUrl = statusUrl; }
    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String frontendBaseUrl) { this.frontendBaseUrl = frontendBaseUrl; }
}
