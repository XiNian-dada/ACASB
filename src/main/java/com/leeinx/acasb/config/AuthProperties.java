package com.leeinx.acasb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthProperties {
    private boolean enabled = true;
    private String secret = "";
    private long expiresHours = 24L * 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiresHours() {
        return expiresHours;
    }

    public void setExpiresHours(long expiresHours) {
        this.expiresHours = expiresHours;
    }
}
