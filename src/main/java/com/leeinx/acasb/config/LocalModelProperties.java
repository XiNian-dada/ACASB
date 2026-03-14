package com.leeinx.acasb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "local.model")
public class LocalModelProperties {
    private boolean predictionEnabled = false;
}
