package com.leeinx.acasb.config;

import com.leeinx.acasb.jwt.JwtUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class JwtBootstrap {
    private final AuthProperties authProperties;

    public JwtBootstrap(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @PostConstruct
    public void initializeJwt() {
        JwtUtils.configure(authProperties.getSecret(), authProperties.getExpiresHours());
    }
}
