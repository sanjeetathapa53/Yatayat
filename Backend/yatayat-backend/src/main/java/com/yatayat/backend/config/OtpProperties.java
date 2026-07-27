package com.yatayat.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "yatayat.auth.otp")
public class OtpProperties {
    private Duration expiry = Duration.ofMinutes(5);
    private Duration resendCooldown = Duration.ofSeconds(60);
    private Duration requestWindow = Duration.ofMinutes(15);
    private int maximumAttempts = 5;
    private int maximumRequestsPerWindow = 5;

    public Duration getExpiry() { return expiry; }
    public void setExpiry(Duration expiry) { this.expiry = expiry; }
    public Duration getResendCooldown() { return resendCooldown; }
    public void setResendCooldown(Duration resendCooldown) { this.resendCooldown = resendCooldown; }
    public Duration getRequestWindow() { return requestWindow; }
    public void setRequestWindow(Duration requestWindow) { this.requestWindow = requestWindow; }
    public int getMaximumAttempts() { return maximumAttempts; }
    public void setMaximumAttempts(int maximumAttempts) { this.maximumAttempts = maximumAttempts; }
    public int getMaximumRequestsPerWindow() { return maximumRequestsPerWindow; }
    public void setMaximumRequestsPerWindow(int value) { this.maximumRequestsPerWindow = value; }
}
