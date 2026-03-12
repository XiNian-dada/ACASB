package com.leeinx.acasb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.config.AiAnalysisProperties;
import com.leeinx.acasb.dto.AiBuildingAnalysis;
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

    public AiBuildingAnalysis analyze(Path imagePath) {
        if (!StringUtils.hasText(aiAnalysisProperties.getApiKey())) {
            return AiBuildingAnalysis.failed(
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
                                            "Return only a valid JSON object with concise, evidence-based guesses. " +
                                            "If a field is uncertain, say unknown instead of fabricating certainty."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "text",
                                                    "text",
                                                    """
                                                    Analyze the uploaded building image and return exactly one JSON object.
                                                    Required JSON schema:
                                                    {
                                                      "building_type": "building category guess",
                                                      "building_type_confidence": 0.0,
                                                      "style": "architectural style guess",
                                                      "style_confidence": 0.0,
                                                      "estimated_era": "estimated historical era",
                                                      "estimated_era_reasoning": "why you inferred this era",
                                                      "roof_type": "roof form or unknown",
                                                      "main_materials": ["material 1", "material 2"],
                                                      "dominant_colors": [
                                                        {"name": "red", "ratio": 0.45, "description": "where it appears"}
                                                      ],
                                                      "key_features": ["feature 1", "feature 2"],
                                                      "summary": "short summary in Chinese"
                                                    }
                                                    
                                                    Requirements:
                                                    - Focus on traditional East Asian / Chinese architecture cues if relevant.
                                                    - Estimate visible dominant color ratios from the building facade or main visible structure, not the full background.
                                                    - Ratios should be between 0 and 1 and roughly sum to 1 across dominant_colors.
                                                    - Keep the answer concise and factual.
                                                    - Do not wrap the JSON in markdown.
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
            String jsonPayload = unwrapJson(content);
            AiBuildingAnalysis analysis = objectMapper.readValue(jsonPayload, AiBuildingAnalysis.class);
            analysis.setEnabled(true);
            analysis.setSuccess(true);
            analysis.setProvider(PROVIDER_NAME);
            analysis.setModel(aiAnalysisProperties.getModel());
            analysis.setError(null);
            return analysis;
        } catch (Exception e) {
            logger.warn("AI building analysis failed for {}: {}", imagePath, e.getMessage());
            return AiBuildingAnalysis.failed(PROVIDER_NAME, aiAnalysisProperties.getModel(), e.getMessage());
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

    private String unwrapJson(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }
}
