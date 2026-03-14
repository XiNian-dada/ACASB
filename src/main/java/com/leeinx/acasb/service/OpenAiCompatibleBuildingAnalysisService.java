package com.leeinx.acasb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.config.AiAnalysisProperties;
import com.leeinx.acasb.dto.AiAnalyzeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiCompatibleBuildingAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiCompatibleBuildingAnalysisService.class);
    private static final String PROVIDER_NAME = "openai-compatible";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiAnalysisProperties aiAnalysisProperties;

    public OpenAiCompatibleBuildingAnalysisService(
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

    public AiAnalyzeResult analyze(Path imagePath) {
        if (!StringUtils.hasText(aiAnalysisProperties.getApiKey())) {
            return AiAnalyzeResult.failed(
                    PROVIDER_NAME,
                    aiAnalysisProperties.getModel(),
                    "AI analysis is enabled but ai.analysis.api-key is empty"
            );
        }

        try {
            String imageMimeType = detectMimeType(imagePath);
            String imageDataUri = buildDataUri(imagePath, imageMimeType);

            Map<String, Object> payload = Map.of(
                    "model", aiAnalysisProperties.getModel(),
                    "temperature", aiAnalysisProperties.getTemperature(),
                    "max_tokens", aiAnalysisProperties.getMaxTokens(),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content",
                                    "You are an expert in Chinese architectural visual analysis. " +
                                            "Give a concise, evidence-based answer in Chinese. " +
                                            "If uncertain, explicitly say it is a guess."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "text",
                                                    "text",
                                                    """
                                                    Analyze the uploaded building image.
                                                    Please describe it in Chinese and cover these points:
                                                    1. 建筑类型推断
                                                    2. 建筑主体可见颜色及大致占比
                                                    3. 建筑风格推断
                                                    4. 年代或时期推断
                                                    5. 你做出判断的关键建筑特征
                                                    
                                                    Requirements:
                                                    - You may answer in plain text or JSON.
                                                    - Do not use markdown code fences.
                                                    - Focus on the main building body, not the full background.
                                                    - Keep it concise, factual, and easy to display directly in an API response.
                                                    """
                                            ),
                                            Map.of(
                                                    "type", "image_url",
                                                    "image_url", Map.of("url", imageDataUri)
                                            )
                                    )
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiAnalysisProperties.getApiKey());

            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiAnalysisProperties.buildChatCompletionsUrl(),
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            String content = extractAssistantContent(response.getBody());
            return AiAnalyzeResult.success(
                    PROVIDER_NAME,
                    aiAnalysisProperties.getModel(),
                    sanitizeOutput(content)
            );
        } catch (Exception e) {
            logger.warn("AI building analysis failed for {}: {}", imagePath, e.getMessage());
            return AiAnalyzeResult.failed(PROVIDER_NAME, aiAnalysisProperties.getModel(), e.getMessage());
        }
    }

    private String detectMimeType(Path imagePath) throws IOException {
        String mimeType = Files.probeContentType(imagePath);
        if (StringUtils.hasText(mimeType)) {
            return mimeType;
        }

        String fileName = imagePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }

    private String buildDataUri(Path imagePath, String mimeType) throws IOException {
        byte[] bytes = Files.readAllBytes(imagePath);
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String extractAssistantContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

        if (contentNode.isTextual()) {
            return contentNode.asText();
        }

        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item.has("text")) {
                    builder.append(item.get("text").asText());
                }
            }
            return builder.toString();
        }

        throw new IOException("Unsupported AI response format: missing choices[0].message.content");
    }

    private String sanitizeOutput(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }
}
