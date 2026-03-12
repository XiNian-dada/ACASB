package com.leeinx.acasb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "python.service")
public class PythonServiceProperties {
    private String scheme = "http";
    private String host = "localhost";
    private int port = 5000;

    public String buildUrl(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return String.format("%s://%s:%d%s", scheme, host, port, normalizedPath);
    }
}
