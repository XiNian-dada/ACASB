package com.leeinx.acasb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.config.AiAnalysisProperties;
import com.leeinx.acasb.dto.AiAnalyzeResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OpenAiCompatibleTextService {
    private static final String PROVIDER_NAME = "openai-compatible";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiAnalysisProperties aiAnalysisProperties;

    public OpenAiCompatibleTextService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            AiAnalysisProperties aiAnalysisProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.aiAnalysisProperties = aiAnalysisProperties;
    }

    public boolean isEnabledByDefault() {
        return aiAnalysisProperties.isEnabled();
    }

    public AiAnalyzeResult generateText(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(aiAnalysisProperties.getApiKey())) {
            return AiAnalyzeResult.failed(
                    PROVIDER_NAME,
                    aiAnalysisProperties.getModel(),
                    "AI analysis is enabled but ai.analysis.api-key is empty"
            );
        }

        try {
            for (RequestVariant requestVariant : buildRequestVariants(systemPrompt, userPrompt)) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBearerAuth(aiAnalysisProperties.getApiKey());

                    ResponseEntity<String> response = restTemplate.postForEntity(
                            requestVariant.url(),
                            new HttpEntity<>(requestVariant.payload(), headers),
                            String.class
                    );
                    return AiAnalyzeResult.success(
                            PROVIDER_NAME,
                            aiAnalysisProperties.getModel(),
                            sanitizeOutput(extractResponseText(response.getBody(), requestVariant.interfaceName()))
                    );
                } catch (Exception ignored) {
                    // Try next compatible request variant.
                }
            }
        } catch (Exception e) {
            return AiAnalyzeResult.failed(PROVIDER_NAME, aiAnalysisProperties.getModel(), e.getMessage());
        }

        return AiAnalyzeResult.failed(PROVIDER_NAME, aiAnalysisProperties.getModel(), "未从兼容接口获得有效文本响应");
    }

    private List<RequestVariant> buildRequestVariants(String systemPrompt, String userPrompt) {
        String apiInterface = aiAnalysisProperties.getApiInterface() == null
                ? "responses"
                : aiAnalysisProperties.getApiInterface().trim().toLowerCase(Locale.ROOT);
        if (!List.of("responses", "chat", "auto").contains(apiInterface)) {
            apiInterface = "auto";
        }

        List<RequestVariant> variants = new ArrayList<>();
        if ("responses".equals(apiInterface) || "auto".equals(apiInterface)) {
            variants.add(new RequestVariant("responses", aiAnalysisProperties.buildResponsesUrl(),
                    buildResponsesPayload(systemPrompt, userPrompt)));
        }
        if ("chat".equals(apiInterface) || "auto".equals(apiInterface)) {
            variants.add(new RequestVariant("chat", aiAnalysisProperties.buildChatCompletionsUrl(),
                    buildChatPayload(systemPrompt, userPrompt)));
        }
        return variants;
    }

    private Map<String, Object> buildResponsesPayload(String systemPrompt, String userPrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiAnalysisProperties.getModel());
        payload.put("instructions", systemPrompt);
        payload.put("temperature", aiAnalysisProperties.getTemperature());
        payload.put("max_output_tokens", aiAnalysisProperties.getMaxTokens());
        payload.put("input", List.of(
                Map.of(
                        "role", "user",
                        "content", List.of(Map.of("type", "input_text", "text", userPrompt))
                )
        ));
        return payload;
    }

    private Map<String, Object> buildChatPayload(String systemPrompt, String userPrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiAnalysisProperties.getModel());
        payload.put("temperature", aiAnalysisProperties.getTemperature());
        payload.put("max_tokens", aiAnalysisProperties.getMaxTokens());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        return payload;
    }

    private String extractResponseText(String responseBody, String interfaceName) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        if ("responses".equals(interfaceName)) {
            String outputText = root.path("output_text").asText("");
            if (StringUtils.hasText(outputText)) {
                return outputText.trim();
            }
            StringBuilder builder = new StringBuilder();
            JsonNode outputNode = root.path("output");
            if (outputNode.isArray()) {
                for (JsonNode item : outputNode) {
                    JsonNode content = item.path("content");
                    if (!content.isArray()) {
                        continue;
                    }
                    for (JsonNode part : content) {
                        String type = part.path("type").asText("");
                        if ("output_text".equals(type) || "text".equals(type)) {
                            builder.append(part.path("text").asText(""));
                        }
                    }
                }
            }
            if (builder.length() > 0) {
                return builder.toString().trim();
            }
            throw new IOException("无法从 responses 接口响应中提取文本");
        }

        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText().trim();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item.has("text")) {
                    builder.append(item.path("text").asText(""));
                }
            }
            if (builder.length() > 0) {
                return builder.toString().trim();
            }
        }
        throw new IOException("无法从 chat/completions 接口响应中提取文本");
    }

    private String sanitizeOutput(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private record RequestVariant(String interfaceName, String url, Map<String, Object> payload) {
    }
}
