package com.leeinx.acasb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.analysis")
public class AiAnalysisProperties {
    private boolean enabled = false;
    private String baseUrl = "https://api.openai.com";
    private String apiInterface = "responses";
    private String responsesPath = "/v1/responses";
    private String chatCompletionsPath = "/v1/chat/completions";
    private String apiKey = "";
    private String model = "gpt-4.1-mini";
    private double temperature = 0.2;
    private int maxTokens = 900;

    public String buildChatCompletionsUrl() {
        return buildUrl(chatCompletionsPath);
    }

    public String buildResponsesUrl() {
        return buildUrl(responsesPath);
    }

    private String buildUrl(String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        String normalizedPath = path.startsWith("/")
                ? path
                : "/" + path;
        return normalizedBaseUrl + normalizedPath;
    }
}
