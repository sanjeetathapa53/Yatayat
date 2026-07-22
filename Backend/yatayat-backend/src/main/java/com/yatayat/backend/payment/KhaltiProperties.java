package com.yatayat.backend.payment;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "yatayat.payments.khalti")
public class KhaltiProperties {
    private boolean enabled;
    private String secretKey = "";
    private String initiateUrl = "https://dev.khalti.com/api/v2/epayment/initiate/";
    private String lookupUrl = "https://dev.khalti.com/api/v2/epayment/lookup/";
    private String frontendBaseUrl = "http://localhost:5173";

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        List<String> missing = new ArrayList<>();
        if (blank(secretKey)) missing.add("secret key");
        if (blank(initiateUrl)) missing.add("initiate URL");
        if (blank(lookupUrl)) missing.add("lookup URL");
        if (blank(frontendBaseUrl)) missing.add("frontend base URL");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Khalti is enabled but required configuration is missing: "
                    + String.join(", ", missing));
        }
        requireOfficialApi(initiateUrl, "initiate URL");
        requireOfficialApi(lookupUrl, "lookup URL");
        requireSafeFrontendBaseUrl(frontendBaseUrl);
    }

    private void requireOfficialApi(String value, String label) {
        URI uri = parse(value, label);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"dev.khalti.com".equalsIgnoreCase(uri.getHost())) {
            throw new IllegalStateException("Khalti " + label
                    + " must use the official HTTPS sandbox API host.");
        }
    }

    private void requireSafeFrontendBaseUrl(String value) {
        URI uri = parse(value, "frontend base URL");
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || host.isBlank() || uri.getUserInfo() != null) {
            throw new IllegalStateException("Khalti frontend base URL is invalid.");
        }
        if ("https".equalsIgnoreCase(scheme)) return;
        boolean localDevelopmentHost = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host);
        if (!"http".equalsIgnoreCase(scheme) || !localDevelopmentHost) {
            throw new IllegalStateException(
                    "Khalti frontend base URL must use HTTPS, except HTTP localhost is allowed for local development.");
        }
    }

    private URI parse(String value, String label) {
        try { return URI.create(value.trim()); }
        catch (RuntimeException exception) { throw new IllegalStateException("Khalti " + label + " is invalid."); }
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getInitiateUrl() { return initiateUrl; }
    public void setInitiateUrl(String initiateUrl) { this.initiateUrl = initiateUrl; }
    public String getLookupUrl() { return lookupUrl; }
    public void setLookupUrl(String lookupUrl) { this.lookupUrl = lookupUrl; }
    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String frontendBaseUrl) { this.frontendBaseUrl = frontendBaseUrl; }
}
