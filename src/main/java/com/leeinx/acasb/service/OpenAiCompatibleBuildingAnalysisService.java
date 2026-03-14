package com.leeinx.acasb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.config.AiAnalysisProperties;
import com.leeinx.acasb.dto.AiAnalyzeResult;
import com.leeinx.acasb.dto.AiStructuredAnalysisResult;
import com.leeinx.acasb.dto.DatasetColorDistributionItem;
import com.leeinx.acasb.dto.DatasetImageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OpenAiCompatibleBuildingAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiCompatibleBuildingAnalysisService.class);
    private static final String PROVIDER_NAME = "openai-compatible";
    private static final List<String> PROVINCES = List.of(
            "北京市", "天津市", "上海市", "重庆市", "河北省", "山西省", "辽宁省", "吉林省", "黑龙江省",
            "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省", "河南省", "湖北省", "湖南省",
            "广东省", "海南省", "四川省", "贵州省", "云南省", "陕西省", "甘肃省", "青海省", "台湾省",
            "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
            "香港特别行政区", "澳门特别行政区"
    );
    private static final Map<String, String> PROVINCE_ALIASES = Map.ofEntries(
            Map.entry("北京", "北京市"),
            Map.entry("天津", "天津市"),
            Map.entry("上海", "上海市"),
            Map.entry("重庆", "重庆市"),
            Map.entry("河北", "河北省"),
            Map.entry("山西", "山西省"),
            Map.entry("辽宁", "辽宁省"),
            Map.entry("吉林", "吉林省"),
            Map.entry("黑龙江", "黑龙江省"),
            Map.entry("江苏", "江苏省"),
            Map.entry("浙江", "浙江省"),
            Map.entry("安徽", "安徽省"),
            Map.entry("福建", "福建省"),
            Map.entry("江西", "江西省"),
            Map.entry("山东", "山东省"),
            Map.entry("河南", "河南省"),
            Map.entry("湖北", "湖北省"),
            Map.entry("湖南", "湖南省"),
            Map.entry("广东", "广东省"),
            Map.entry("海南", "海南省"),
            Map.entry("四川", "四川省"),
            Map.entry("贵州", "贵州省"),
            Map.entry("云南", "云南省"),
            Map.entry("陕西", "陕西省"),
            Map.entry("甘肃", "甘肃省"),
            Map.entry("青海", "青海省"),
            Map.entry("台湾", "台湾省"),
            Map.entry("内蒙古", "内蒙古自治区"),
            Map.entry("广西", "广西壮族自治区"),
            Map.entry("西藏", "西藏自治区"),
            Map.entry("宁夏", "宁夏回族自治区"),
            Map.entry("新疆", "新疆维吾尔自治区"),
            Map.entry("香港", "香港特别行政区"),
            Map.entry("澳门", "澳门特别行政区")
    );
    private static final List<String> DYNASTIES = List.of("唐", "宋", "元", "明", "清");
    private static final Map<String, String> DYNASTY_ALIASES = Map.of(
            "唐代", "唐",
            "宋代", "宋",
            "元代", "元",
            "明代", "明",
            "清代", "清"
    );
    private static final List<String> BUILDING_RANKS = List.of("皇家", "王公", "官员", "平民");
    private static final Map<String, String> BUILDING_RANK_ALIASES = Map.ofEntries(
            Map.entry("皇室", "皇家"),
            Map.entry("宫廷", "皇家"),
            Map.entry("宫殿", "皇家"),
            Map.entry("王府", "王公"),
            Map.entry("王公贵族", "王公"),
            Map.entry("贵族", "王公"),
            Map.entry("官府", "官员"),
            Map.entry("官署", "官员"),
            Map.entry("官式", "官员"),
            Map.entry("士绅", "官员"),
            Map.entry("民居", "平民"),
            Map.entry("百姓", "平民"),
            Map.entry("民间", "平民")
    );
    private static final Set<String> ALLOWED_SCENE_TYPES = Set.of("建筑外观", "建筑群", "街景", "园林", "室内", "其他");
    private static final List<String> BANNED_HEDGING_PHRASES = List.of(
            "难以确定", "无法推测", "无法判断", "不能判断", "不确定", "估计", "疑似", "可能", "大概", "仅凭"
    );
    private static final String SYSTEM_PROMPT =
            "你是一个严格遵守格式的中国建筑图像分析器。" +
                    "你的唯一任务是基于图片本身的可见视觉信息，输出一个可被标准 JSON 解析的对象。" +
                    "你绝不能输出 markdown、代码块、额外解释、前后缀文本或注释。" +
                    "你必须使用直接、肯定、专业的中文表述，不得在自然语言字段里使用难以确定、无法判断、估计、疑似、可能、大概等犹豫措辞。" +
                    "不确定性只能通过 province_confidence 和 needs_manual_review 表达。";

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
        AiStructuredAnalysisResult structuredResult = analyzeStructured(imagePath);
        if (structuredResult.isSuccess()) {
            return AiAnalyzeResult.success(
                    structuredResult.getProvider(),
                    structuredResult.getModel(),
                    structuredResult.getRawContent()
            );
        }
        return AiAnalyzeResult.failed(
                structuredResult.getProvider(),
                structuredResult.getModel(),
                structuredResult.getError()
        );
    }

    public AiStructuredAnalysisResult analyzeStructured(Path imagePath) {
        DatasetImageMetadata fallback = buildFailureAnalysis(imagePath, "AI 解析失败");
        if (!StringUtils.hasText(aiAnalysisProperties.getApiKey())) {
            String error = "AI analysis is enabled but ai.analysis.api-key is empty";
            fallback.setErrorMessage(error);
            return AiStructuredAnalysisResult.failed(PROVIDER_NAME, aiAnalysisProperties.getModel(), error, fallback);
        }

        try {
            String imageMimeType = detectMimeType(imagePath);
            String imageDataUri = buildDataUri(imagePath, imageMimeType);
            String relativePath = resolveRelativePath(imagePath);
            String userPrompt = buildUserPrompt(relativePath);
            List<RequestVariant> requestVariants = buildRequestVariants(userPrompt, imageDataUri);
            String lastError = "unknown error";

            for (RequestVariant requestVariant : requestVariants) {
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            requestVariant.url(),
                            new HttpEntity<>(requestVariant.payload(), buildHeaders()),
                            String.class
                    );
                    String rawContent = extractResponseText(response.getBody(), requestVariant.interfaceName());
                    DatasetImageMetadata normalized = normalizeAnalysis(parseJsonObject(rawContent), imagePath);
                    return AiStructuredAnalysisResult.success(
                            PROVIDER_NAME,
                            aiAnalysisProperties.getModel(),
                            sanitizeOutput(rawContent),
                            normalized
                    );
                } catch (RestClientResponseException e) {
                    lastError = "HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString();
                    logger.warn("AI variant {} failed for {}: {}", requestVariant.interfaceName(), imagePath, lastError);
                } catch (Exception e) {
                    lastError = e.getMessage();
                    logger.warn("AI variant {} failed for {}: {}", requestVariant.interfaceName(), imagePath, lastError);
                }
            }

            fallback.setErrorMessage(lastError);
            return AiStructuredAnalysisResult.failed(PROVIDER_NAME, aiAnalysisProperties.getModel(), lastError, fallback);
        } catch (Exception e) {
            logger.warn("AI building analysis failed for {}: {}", imagePath, e.getMessage());
            fallback.setErrorMessage(e.getMessage());
            return AiStructuredAnalysisResult.failed(PROVIDER_NAME, aiAnalysisProperties.getModel(), e.getMessage(), fallback);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiAnalysisProperties.getApiKey());
        return headers;
    }

    private List<RequestVariant> buildRequestVariants(String userPrompt, String imageDataUri) {
        String apiInterface = aiAnalysisProperties.getApiInterface() == null
                ? "responses"
                : aiAnalysisProperties.getApiInterface().trim().toLowerCase(Locale.ROOT);
        if (!List.of("responses", "chat", "auto").contains(apiInterface)) {
            apiInterface = "auto";
        }

        List<RequestVariant> variants = new ArrayList<>();
        if ("responses".equals(apiInterface) || "auto".equals(apiInterface)) {
            variants.add(new RequestVariant("responses", aiAnalysisProperties.buildResponsesUrl(),
                    buildResponsesPayload(userPrompt, imageDataUri, "json_schema")));
            variants.add(new RequestVariant("responses", aiAnalysisProperties.buildResponsesUrl(),
                    buildResponsesPayload(userPrompt, imageDataUri, "json_object")));
            variants.add(new RequestVariant("responses", aiAnalysisProperties.buildResponsesUrl(),
                    buildResponsesPayload(userPrompt, imageDataUri, "none")));
        }
        if ("chat".equals(apiInterface) || "auto".equals(apiInterface)) {
            variants.add(new RequestVariant("chat", aiAnalysisProperties.buildChatCompletionsUrl(),
                    buildChatPayload(userPrompt, imageDataUri, "json_schema")));
            variants.add(new RequestVariant("chat", aiAnalysisProperties.buildChatCompletionsUrl(),
                    buildChatPayload(userPrompt, imageDataUri, "json_object")));
            variants.add(new RequestVariant("chat", aiAnalysisProperties.buildChatCompletionsUrl(),
                    buildChatPayload(userPrompt, imageDataUri, "none")));
        }
        return variants;
    }

    private Map<String, Object> buildChatPayload(String userPrompt, String imageDataUri, String formatMode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiAnalysisProperties.getModel());
        payload.put("temperature", aiAnalysisProperties.getTemperature());
        payload.put("max_tokens", aiAnalysisProperties.getMaxTokens());

        List<Object> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        List<Object> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text", userPrompt));
        userContent.add(Map.of("type", "image_url", "image_url", Map.of("url", imageDataUri, "detail", "high")));
        messages.add(Map.of("role", "user", "content", userContent));
        payload.put("messages", messages);

        if ("json_schema".equals(formatMode)) {
            payload.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "china_architecture_analysis",
                            "strict", true,
                            "schema", buildResponseJsonSchema()
                    )
            ));
        } else if ("json_object".equals(formatMode)) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        return payload;
    }

    private Map<String, Object> buildResponsesPayload(String userPrompt, String imageDataUri, String formatMode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiAnalysisProperties.getModel());
        payload.put("instructions", SYSTEM_PROMPT);
        payload.put("temperature", aiAnalysisProperties.getTemperature());
        payload.put("max_output_tokens", aiAnalysisProperties.getMaxTokens());

        List<Object> input = new ArrayList<>();
        List<Object> content = new ArrayList<>();
        content.add(Map.of("type", "input_text", "text", userPrompt));
        content.add(Map.of("type", "input_image", "image_url", imageDataUri, "detail", "high"));
        input.add(Map.of("role", "user", "content", content));
        payload.put("input", input);

        if ("json_schema".equals(formatMode)) {
            payload.put("text", Map.of(
                    "format", Map.of(
                            "type", "json_schema",
                            "name", "china_architecture_analysis",
                            "strict", true,
                            "schema", buildResponseJsonSchema()
                    )
            ));
        } else if ("json_object".equals(formatMode)) {
            payload.put("text", Map.of("format", Map.of("type", "json_object")));
        }
        return payload;
    }

    private Map<String, Object> buildResponseJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(
                "province_level_region",
                "province_confidence",
                "dynasty_guess",
                "building_rank",
                "scene_type",
                "building_present",
                "building_primary_colors",
                "building_color_distribution",
                "architecture_style",
                "scene_description",
                "reasoning",
                "needs_manual_review"
        ));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("province_level_region", Map.of("type", "string", "enum", PROVINCES));
        properties.put("province_confidence", Map.of("type", "number", "minimum", 0, "maximum", 1));
        properties.put("dynasty_guess", Map.of("type", "string", "enum", DYNASTIES));
        properties.put("building_rank", Map.of("type", "string", "enum", BUILDING_RANKS));
        properties.put("scene_type", Map.of("type", "string", "enum", List.of("建筑外观", "建筑群", "街景", "园林", "室内", "其他")));
        properties.put("building_present", Map.of("type", "boolean"));
        properties.put("building_primary_colors", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "maxItems", 5
        ));

        Map<String, Object> ratioItem = new LinkedHashMap<>();
        ratioItem.put("type", "object");
        ratioItem.put("additionalProperties", false);
        ratioItem.put("required", List.of("color", "ratio"));
        ratioItem.put("properties", Map.of(
                "color", Map.of("type", "string"),
                "ratio", Map.of("type", "number", "minimum", 0, "maximum", 1)
        ));
        properties.put("building_color_distribution", Map.of(
                "type", "array",
                "items", ratioItem,
                "minItems", 1,
                "maxItems", 5
        ));
        properties.put("architecture_style", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "minItems", 1,
                "maxItems", 4
        ));
        properties.put("scene_description", Map.of("type", "string"));
        properties.put("reasoning", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "minItems", 2,
                "maxItems", 5
        ));
        properties.put("needs_manual_review", Map.of("type", "boolean"));
        schema.put("properties", properties);
        return schema;
    }

    private String buildUserPrompt(String relativePath) {
        return """
                请分析这张图片，并完成建筑与地域推断。

                前提：
                1. 该图片一定拍摄于中国境内。
                2. 你必须给出最可能的一个中国省级行政区名称，精确到省级。
                3. 只能依据图片本身可见内容判断，严禁使用文件名、目录名、EXIF、外部知识库或联网搜索结果作为证据。

                省级行政区候选列表：
                %s

                你必须只返回一个 JSON object，且键名必须严格等于以下内容，不能增加、删除或改名：
                {
                  "province_level_region": "字符串，必须是上述候选列表中的一个标准名称",
                  "province_confidence": 0.0,
                  "dynasty_guess": "唐|宋|元|明|清",
                  "building_rank": "皇家|王公|官员|平民",
                  "scene_type": "建筑外观|建筑群|街景|园林|室内|其他",
                  "building_present": true,
                  "building_primary_colors": ["颜色1", "颜色2"],
                  "building_color_distribution": [{"color": "红色", "ratio": 0.55}, {"color": "灰色", "ratio": 0.30}],
                  "architecture_style": ["风格1", "风格2"],
                  "scene_description": "单行中文字符串，80到160字，描述画面主体、建筑主体颜色占比、建筑风格、材质、朝代和等级",
                  "reasoning": ["依据1", "依据2"],
                  "needs_manual_review": false
                }

                硬性规则：
                1. province_level_region 必须从候选列表中选一个，不能写“未知”“不确定”“中国”。
                2. province_confidence 必须是 0 到 1 之间的小数。
                3. dynasty_guess 必须从 唐、宋、元、明、清 五个中选一个，不得写其他朝代名称。
                4. building_rank 必须从 皇家、王公、官员、平民 四个中选一个，不得写其他等级。
                5. building_primary_colors 必须是 0 到 5 个中文颜色词，不要写材质。
                6. building_color_distribution 必须填写建筑主体可见颜色占比，ratio 为 0 到 1 之间的小数，总和控制在 1 左右；只统计建筑主体，不统计天空、树木、行人和地面。
                7. architecture_style 必须是 1 到 4 个简短风格标签，并明确属于近代以前的传统建筑体系，例如：皇家官式古建、北方寺观古建、徽派古建、闽南红砖古建、藏式古建、岭南祠庙古建。
                8. scene_description 必须明确写出建筑主体颜色占比，例如“红色约占 50%%，灰色约占 30%%”，并明确写出朝代判断和建筑等级判断。
                9. reasoning 必须是 2 到 5 条，只能写肉眼可见的视觉依据，且禁止出现“难以确定”“无法推测”“无法判断”“估计”“可能”“疑似”“大概”等措辞。
                10. 允许你在合理范围内做建筑类型、省份、朝代和等级推断，但输出语言必须直接、肯定，不得自述为猜测或估计。
                11. 如果建筑不明显，scene_type 也要按画面主要内容填写，并尽力给出建筑颜色/风格判断；无法确认时 architecture_style 填 ["未识别"]。
                12. 当 province_confidence < 0.55，或建筑特征不明显时，needs_manual_review 必须为 true。
                13. 输出必须是合法 JSON，不能带 markdown 代码块。

                图片相对路径：%s
                """.formatted(String.join("、", PROVINCES), relativePath);
    }

    private String detectMimeType(Path imagePath) throws IOException {
        String mimeType = Files.probeContentType(imagePath);
        if (StringUtils.hasText(mimeType)) {
            return mimeType;
        }

        String fileName = imagePath.getFileName().toString().toLowerCase(Locale.ROOT);
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
                        String partType = part.path("type").asText("");
                        if ("output_text".equals(partType) || "text".equals(partType)) {
                            String text = part.path("text").asText("");
                            if (StringUtils.hasText(text)) {
                                builder.append(text);
                            }
                        } else if ("refusal".equals(partType)) {
                            throw new IOException("模型拒绝输出: " + part.path("refusal").asText(""));
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

    private Map<String, Object> parseJsonObject(String text) throws IOException {
        String cleaned = stripCodeFences(text);
        try {
            return objectMapper.readValue(cleaned, new TypeReference<>() {
            });
        } catch (IOException ignored) {
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw ignored;
            }
            return objectMapper.readValue(cleaned.substring(start, end + 1), new TypeReference<>() {
            });
        }
    }

    private String stripCodeFences(String text) {
        String trimmed = sanitizeOutput(text);
        if (trimmed.startsWith("```")) {
            String[] lines = trimmed.split("\\R");
            int start = lines.length > 0 ? 1 : 0;
            int end = lines.length > 1 && lines[lines.length - 1].startsWith("```") ? lines.length - 1 : lines.length;
            return String.join("\n", java.util.Arrays.copyOfRange(lines, start, end)).trim();
        }
        return trimmed;
    }

    private String sanitizeOutput(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private DatasetImageMetadata normalizeAnalysis(Map<String, Object> raw, Path imagePath) {
        double provinceConfidence = normalizeConfidence(raw.get("province_confidence"));
        List<String> architectureStyle = uniqueTrimmedStrings(raw.get("architecture_style"), 4);
        if (architectureStyle.isEmpty()) {
            architectureStyle = List.of("未识别");
        }

        List<String> buildingPrimaryColors = uniqueTrimmedStrings(raw.get("building_primary_colors"), 5);
        List<DatasetColorDistributionItem> colorDistribution =
                normalizeColorDistribution(raw.get("building_color_distribution"), buildingPrimaryColors);
        if (buildingPrimaryColors.isEmpty()) {
            buildingPrimaryColors = colorDistribution.stream()
                    .map(DatasetColorDistributionItem::getColor)
                    .filter(StringUtils::hasText)
                    .toList();
        }

        DatasetImageMetadata metadata = new DatasetImageMetadata();
        metadata.setProvinceLevelRegion(normalizeProvince(raw.get("province_level_region")));
        metadata.setProvinceConfidence(provinceConfidence);
        metadata.setDynastyGuess(normalizeDynasty(raw.get("dynasty_guess")));
        metadata.setBuildingRank(normalizeBuildingRank(raw.get("building_rank")));
        metadata.setSceneType(normalizeSceneType(raw.get("scene_type")));
        metadata.setBuildingPresent(normalizeBoolean(raw.get("building_present")));
        metadata.setBuildingPrimaryColors(buildingPrimaryColors);
        metadata.setBuildingColorDistribution(colorDistribution);
        metadata.setArchitectureStyle(architectureStyle);
        metadata.setSceneDescription(normalizeDescription(raw.get("scene_description")));
        metadata.setReasoning(normalizeReasoning(raw.get("reasoning")));
        metadata.setNeedsManualReview(normalizeBoolean(raw.get("needs_manual_review")) || provinceConfidence < 0.55);
        metadata.setFileName(imagePath.getFileName() == null ? resolveRelativePath(imagePath) : imagePath.getFileName().toString());
        metadata.setRelativePath(resolveRelativePath(imagePath));
        metadata.setAnalysisStatus("success");
        metadata.setErrorMessage("");
        return polishAnalysis(metadata);
    }

    private DatasetImageMetadata polishAnalysis(DatasetImageMetadata analysis) {
        analysis.setArchitectureStyle(ensureTraditionalStyles(
                analysis.getArchitectureStyle(),
                Boolean.TRUE.equals(analysis.getBuildingPresent())
        ));

        String styleText = String.join(" ", analysis.getArchitectureStyle());
        if (styleText.contains("皇家") || styleText.contains("宫殿")) {
            analysis.setBuildingRank("皇家");
        } else if (styleText.contains("王府") || styleText.contains("王公")) {
            analysis.setBuildingRank("王公");
        } else if (styleText.contains("官式") || styleText.contains("官署")) {
            analysis.setBuildingRank("官员");
        }

        String sceneDescription = analysis.getSceneDescription();
        if (containsBannedHedging(sceneDescription)
                || !sceneDescription.contains("占")
                || !sceneDescription.contains("%")
                || (!sceneDescription.contains("近代以前") && !sceneDescription.contains("传统") && !sceneDescription.contains("古建"))
                || !sceneDescription.contains(analysis.getDynastyGuess())
                || !sceneDescription.contains(analysis.getBuildingRank())) {
            analysis.setSceneDescription(buildAssertiveSceneDescription(analysis));
        }

        List<String> reasoning = analysis.getReasoning();
        boolean invalidReasoning = reasoning == null
                || reasoning.size() < 2
                || reasoning.stream().anyMatch(this::containsBannedHedging)
                || reasoning.stream().noneMatch(item -> item.contains("占") || item.contains("%"));
        if (invalidReasoning) {
            analysis.setReasoning(buildAssertiveReasoning(analysis));
        }

        return analysis;
    }

    private DatasetImageMetadata buildFailureAnalysis(Path imagePath, String error) {
        DatasetImageMetadata metadata = new DatasetImageMetadata();
        metadata.setProvinceLevelRegion("北京市");
        metadata.setProvinceConfidence(0.0);
        metadata.setDynastyGuess("清");
        metadata.setBuildingRank("官员");
        metadata.setSceneType("其他");
        metadata.setBuildingPresent(false);
        metadata.setBuildingPrimaryColors(List.of());

        DatasetColorDistributionItem colorItem = new DatasetColorDistributionItem();
        colorItem.setColor("灰色");
        colorItem.setRatio(1.0);
        metadata.setBuildingColorDistribution(List.of(colorItem));
        metadata.setArchitectureStyle(List.of("未识别"));
        metadata.setSceneDescription("分析失败，需要人工复核。");
        metadata.setReasoning(List.of("接口调用失败，未获得有效视觉分析结果。", "建议重新运行或人工复核。"));
        metadata.setNeedsManualReview(true);
        metadata.setFileName(imagePath.getFileName() == null ? resolveRelativePath(imagePath) : imagePath.getFileName().toString());
        metadata.setRelativePath(resolveRelativePath(imagePath));
        metadata.setAnalysisStatus("failed");
        metadata.setErrorMessage(error);
        return metadata;
    }

    private String resolveRelativePath(Path imagePath) {
        if (imagePath == null) {
            return "";
        }
        Path fileName = imagePath.getFileName();
        return fileName == null ? imagePath.toString() : fileName.toString();
    }

    private String normalizeProvince(Object rawValue) {
        String text = safeText(rawValue);
        if (PROVINCES.contains(text)) {
            return text;
        }
        if (PROVINCE_ALIASES.containsKey(text)) {
            return PROVINCE_ALIASES.get(text);
        }
        return "北京市";
    }

    private String normalizeDynasty(Object rawValue) {
        String text = safeText(rawValue);
        if (DYNASTIES.contains(text)) {
            return text;
        }
        if (DYNASTY_ALIASES.containsKey(text)) {
            return DYNASTY_ALIASES.get(text);
        }
        return "清";
    }

    private String normalizeBuildingRank(Object rawValue) {
        String text = safeText(rawValue);
        if (BUILDING_RANKS.contains(text)) {
            return text;
        }
        if (BUILDING_RANK_ALIASES.containsKey(text)) {
            return BUILDING_RANK_ALIASES.get(text);
        }
        return "官员";
    }

    private double normalizeConfidence(Object rawValue) {
        try {
            double value = Double.parseDouble(String.valueOf(rawValue));
            if (value < 0) {
                return 0;
            }
            if (value > 1) {
                return 1;
            }
            return round4(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private String normalizeSceneType(Object rawValue) {
        String text = safeText(rawValue);
        if (ALLOWED_SCENE_TYPES.contains(text)) {
            return text;
        }
        if (text.contains("室内")) {
            return "室内";
        }
        if (text.contains("街")) {
            return "街景";
        }
        if (text.contains("园")) {
            return "园林";
        }
        if (text.contains("群")) {
            return "建筑群";
        }
        if (text.contains("建筑")) {
            return "建筑外观";
        }
        return "其他";
    }

    private boolean normalizeBoolean(Object rawValue) {
        if (rawValue instanceof Boolean value) {
            return value;
        }
        if (rawValue instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = safeText(rawValue).toLowerCase(Locale.ROOT);
        return List.of("true", "1", "yes", "y", "是").contains(text);
    }

    private List<String> uniqueTrimmedStrings(Object rawValue, int limit) {
        if (!(rawValue instanceof List<?> listValue)) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (Object item : listValue) {
            String text = safeText(item);
            if (!StringUtils.hasText(text) || !seen.add(text)) {
                continue;
            }
            result.add(text);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<DatasetColorDistributionItem> normalizeColorDistribution(Object rawValue, List<String> fallbackColors) {
        List<DatasetColorDistributionItem> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (rawValue instanceof List<?> listValue) {
            for (Object item : listValue) {
                if (!(item instanceof Map<?, ?> mapValue)) {
                    continue;
                }
                String color = safeText(mapValue.get("color"));
                if (!StringUtils.hasText(color) || !seen.add(color)) {
                    continue;
                }
                DatasetColorDistributionItem distributionItem = new DatasetColorDistributionItem();
                distributionItem.setColor(color);
                distributionItem.setRatio(normalizeRatio(mapValue.get("ratio")));
                normalized.add(distributionItem);
                if (normalized.size() >= 5) {
                    break;
                }
            }
        }

        if (normalized.isEmpty()) {
            List<String> fallback = fallbackColors.isEmpty() ? List.of("灰色") : fallbackColors.stream().limit(5).toList();
            double equalRatio = round4(1.0 / fallback.size());
            normalized = fallback.stream().map(color -> {
                DatasetColorDistributionItem item = new DatasetColorDistributionItem();
                item.setColor(color);
                item.setRatio(equalRatio);
                return item;
            }).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        double total = normalized.stream().map(DatasetColorDistributionItem::getRatio).filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            double equalRatio = round4(1.0 / normalized.size());
            normalized.forEach(item -> item.setRatio(equalRatio));
        } else {
            for (DatasetColorDistributionItem item : normalized) {
                item.setRatio(round4(item.getRatio() / total));
            }
            double adjustedTotal = normalized.stream().map(DatasetColorDistributionItem::getRatio).mapToDouble(Double::doubleValue).sum();
            DatasetColorDistributionItem lastItem = normalized.get(normalized.size() - 1);
            lastItem.setRatio(round4(lastItem.getRatio() + (1.0 - adjustedTotal)));
        }
        return normalized;
    }

    private double normalizeRatio(Object rawValue) {
        try {
            double value = Double.parseDouble(String.valueOf(rawValue));
            if (value > 1 && value <= 100) {
                value = value / 100.0;
            }
            if (value < 0) {
                return 0;
            }
            if (value > 1) {
                return 1;
            }
            return value;
        } catch (Exception e) {
            return 0;
        }
    }

    private String normalizeDescription(Object rawValue) {
        String text = safeText(rawValue).replaceAll("\\s+", " ").trim();
        return StringUtils.hasText(text) ? text : "图像内容需要人工复核，暂未得到稳定描述。";
    }

    private List<String> normalizeReasoning(Object rawValue) {
        List<String> reasons = uniqueTrimmedStrings(rawValue, 5);
        List<String> normalized = new ArrayList<>(reasons);
        while (normalized.size() < 2) {
            normalized.add("可见视觉依据不足，建议人工复核。");
        }
        return normalized;
    }

    private List<String> ensureTraditionalStyles(List<String> styles, boolean buildingPresent) {
        if (styles == null || styles.isEmpty()) {
            return List.of(buildingPresent ? "近代以前传统建筑" : "未识别");
        }
        List<String> keywords = List.of(
                "古建", "传统", "官式", "寺观", "祠庙", "殿阁", "宫殿", "民居", "楼阁", "园林", "院落", "藏式", "徽派", "闽南", "岭南", "晋派"
        );
        boolean hasTraditionalKeyword = styles.stream().anyMatch(style ->
                keywords.stream().anyMatch(style::contains));
        if (hasTraditionalKeyword) {
            return styles;
        }
        if (styles.size() == 1 && "未识别".equals(styles.get(0))) {
            return List.of(buildingPresent ? "近代以前传统建筑" : "未识别");
        }

        List<String> result = new ArrayList<>();
        result.add("近代以前传统建筑");
        result.addAll(styles.stream().limit(3).toList());
        return result;
    }

    private String buildAssertiveSceneDescription(DatasetImageMetadata analysis) {
        return "画面主体为" + analysis.getSceneType() + "中的近代以前传统建筑，建筑主体色彩分布为"
                + formatColorDistributionText(analysis.getBuildingColorDistribution())
                + "，屋顶、立柱、墙面与台基层次清楚，整体呈现"
                + String.join("、", analysis.getArchitectureStyle())
                + "特征，材质以木构、灰瓦与石质构件组合为主，朝代判断归入"
                + analysis.getDynastyGuess()
                + "，建筑等级归入"
                + analysis.getBuildingRank()
                + "，整体审美与"
                + analysis.getProvinceLevelRegion()
                + "传统建筑风格相符。";
    }

    private List<String> buildAssertiveReasoning(DatasetImageMetadata analysis) {
        return List.of(
                "建筑主体颜色分布清楚，" + formatColorDistributionText(analysis.getBuildingColorDistribution())
                        + "，色彩主要集中在屋顶、立柱、墙面与台基等核心构件。",
                "屋顶形制、檐口层次、门窗比例与装饰构件明确，整体属于近代以前的"
                        + analysis.getDynastyGuess()
                        + String.join("、", analysis.getArchitectureStyle())
                        + "体系。",
                "主体构图、构件比例与色彩组织共同指向"
                        + analysis.getProvinceLevelRegion()
                        + "范围内的"
                        + analysis.getBuildingRank()
                        + "等级传统建筑风格表达。"
        );
    }

    private String formatColorDistributionText(List<DatasetColorDistributionItem> colorDistribution) {
        List<String> parts = new ArrayList<>();
        for (DatasetColorDistributionItem item : colorDistribution) {
            int percent = Math.max(1, (int) Math.round((item.getRatio() == null ? 0 : item.getRatio()) * 100));
            parts.add(item.getColor() + "约占" + percent + "%");
        }
        return String.join("，", parts);
    }

    private boolean containsBannedHedging(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return BANNED_HEDGING_PHRASES.stream().anyMatch(text::contains);
    }

    private String safeText(Object rawValue) {
        return rawValue == null ? "" : String.valueOf(rawValue).trim();
    }

    private double round4(double value) {
        return Math.round(value * 10000d) / 10000d;
    }

    private record RequestVariant(String interfaceName, String url, Map<String, Object> payload) {
    }
}
