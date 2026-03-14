package com.leeinx.acasb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.dto.AiAnalyzeResult;
import com.leeinx.acasb.dto.ExperienceColorSelection;
import com.leeinx.acasb.dto.ExperienceValidateRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExperienceService {
    private final OpenAiCompatibleTextService openAiCompatibleTextService;
    private final ObjectMapper objectMapper;

    public ExperienceService(
            OpenAiCompatibleTextService openAiCompatibleTextService,
            ObjectMapper objectMapper) {
        this.openAiCompatibleTextService = openAiCompatibleTextService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getRankRules(String rankId) {
        RankRule rule = resolveRule(rankId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rankId", rule.id());
        result.put("rankName", rule.name());
        result.put("maxColors", rule.maxColors());
        result.put("availableColors", rule.colors());
        result.put("note", "当前规则为礼制体验启发式色彩池，适合前端互动页初始化。");
        return result;
    }

    public Map<String, Object> validate(ExperienceValidateRequest request, Boolean enableAi) {
        RankRule rule = resolveRule(request == null ? null : request.getRankId());
        List<ExperienceColorSelection> selections = request == null || request.getSelections() == null
                ? List.of()
                : request.getSelections().stream().filter(item -> item != null && StringUtils.hasText(item.getColorHex())).toList();

        List<Violation> violations = new ArrayList<>();
        for (ExperienceColorSelection selection : selections) {
            ColorPrivilege privilege = findColorPrivilege(selection.getColorHex());
            if (privilege.minRankOrder() > rule.rankOrder()) {
                violations.add(new Violation(selection.getPart(), selection.getColorHex(), privilege));
            }
        }

        boolean valid = violations.isEmpty() && selections.size() <= rule.maxColors();
        String resultLevel = resolveResultLevel(rule, violations, selections.size());
        String resultTitle = valid ? "合乎礼制" : ("danger".equals(resultLevel) ? "僭越警告！" : "存在越制风险");

        String feedback = valid
                ? "当前配色与" + rule.name() + "的礼制范围基本一致，可继续围绕 " + summarizeAllowedColors(rule.colors()) + " 做细化搭配。"
                : buildViolationFeedback(rule, violations, selections.size());
        String knowledgePoint = valid
                ? "历史知识：礼制色彩不仅区分身份等级，也会随地域、建筑类型和时代工艺发生细微变化。"
                : buildKnowledgePoint(violations);

        if (shouldEnableAi(enableAi)) {
            AiAnalyzeResult aiResult = enhanceValidationWithAi(rule, selections, valid, resultLevel, feedback, knowledgePoint);
            if (aiResult.isSuccess() && StringUtils.hasText(aiResult.getContent())) {
                Map<String, String> aiFields = parseAiValidation(aiResult.getContent());
                feedback = aiFields.getOrDefault("feedback", feedback);
                knowledgePoint = aiFields.getOrDefault("knowledgePoint", knowledgePoint);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isValid", valid);
        result.put("resultTitle", resultTitle);
        result.put("resultLevel", resultLevel);
        result.put("feedback", feedback);
        result.put("knowledgePoint", knowledgePoint);
        result.put("rankName", rule.name());
        result.put("selectionCount", selections.size());
        result.put("violations", violations.stream().map(Violation::toMap).toList());
        return result;
    }

    private boolean shouldEnableAi(Boolean enableAi) {
        return enableAi != null ? enableAi : openAiCompatibleTextService.isEnabledByDefault();
    }

    private AiAnalyzeResult enhanceValidationWithAi(
            RankRule rule,
            List<ExperienceColorSelection> selections,
            boolean valid,
            String resultLevel,
            String feedback,
            String knowledgePoint) {
        String systemPrompt = "你是中国古建筑礼制配色讲解助手。请只输出 JSON，不要输出 markdown。";
        String userPrompt = """
                请根据下面的礼制校验结果，生成一段更适合前端展示的反馈。
                只返回 JSON object，键名固定为 feedback 和 knowledgePoint。

                身份等级：%s
                当前结论：%s
                风险级别：%s
                已有反馈：%s
                已有知识点：%s
                用户选择：
                %s
                """.formatted(
                rule.name(),
                valid ? "合规" : "越制",
                resultLevel,
                feedback,
                knowledgePoint,
                selections.stream()
                        .map(item -> (StringUtils.hasText(item.getPart()) ? item.getPart() : "部位")
                                + "=" + item.getColorHex())
                        .collect(Collectors.joining("；"))
        );
        return openAiCompatibleTextService.generateText(systemPrompt, userPrompt);
    }

    private Map<String, String> parseAiValidation(String rawContent) {
        try {
            String text = rawContent.trim();
            if (!text.startsWith("{")) {
                int start = text.indexOf('{');
                int end = text.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    text = text.substring(start, end + 1);
                }
            }
            return objectMapper.readValue(text, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            return Map.of("feedback", rawContent);
        }
    }

    private String resolveResultLevel(RankRule rule, List<Violation> violations, int selectionCount) {
        if (!violations.isEmpty()) {
            boolean severe = violations.stream().anyMatch(item -> item.privilege().minRankOrder() - rule.rankOrder() >= 2);
            return severe ? "danger" : "warning";
        }
        if (selectionCount > rule.maxColors()) {
            return "warning";
        }
        return "success";
    }

    private String buildViolationFeedback(RankRule rule, List<Violation> violations, int selectionCount) {
        if (violations.isEmpty()) {
            return "当前选择的颜色都在礼制范围内，但配色数量超过了 " + rule.name() + " 建议的上限，建议压缩到 " + rule.maxColors() + " 种以内。";
        }

        Violation primary = violations.get(0);
        String part = StringUtils.hasText(primary.part()) ? primary.part() : "关键部位";
        return "作为" + rule.name() + "，您的" + part + "使用了 " + primary.privilege().name()
                + "（" + primary.colorHex() + "），该颜色更接近 " + primary.privilege().reservedFor()
                + " 的礼制配色。建议优先回到 " + summarizeAllowedColors(rule.colors()) + " 这类低阶或常用色系。";
    }

    private String buildKnowledgePoint(List<Violation> violations) {
        if (violations.isEmpty()) {
            return "历史知识：礼制配色常通过屋顶、墙体、彩画和门窗等关键部位体现身份差异。";
        }
        return violations.get(0).privilege().knowledgePoint();
    }

    private String summarizeAllowedColors(List<Map<String, Object>> colors) {
        return colors.stream()
                .limit(3)
                .map(item -> String.valueOf(item.get("name")))
                .collect(Collectors.joining("、"));
    }

    private RankRule resolveRule(String rawRankId) {
        String normalized = rawRankId == null ? "" : rawRankId.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "emperor", "royal", "皇家", "皇帝" -> rankRule(
                    "emperor", "皇家",
                    5, 5,
                    List.of(
                            color("黄色", "#ffbb00", "皇权礼制核心色"),
                            color("红色", "#c4473a", "宫殿墙柱常用色"),
                            color("绿色", "#5e9c45", "高等级建筑彩画与琉璃瓦"),
                            color("蓝色", "#4d79a7", "官式彩画与礼制装饰"),
                            color("灰色", "#888888", "辅色与石构件常见色")
                    )
            );
            case "noble", "prince", "王公", "王公贵族" -> rankRule(
                    "noble", "王公",
                    4, 4,
                    List.of(
                            color("红色", "#c4473a", "高等级府邸常用色"),
                            color("绿色", "#5e9c45", "装饰与屋面辅色"),
                            color("蓝色", "#4d79a7", "彩画和纹饰辅色"),
                            color("灰色", "#888888", "石作与瓦面常见色")
                    )
            );
            case "official", "官员", "官府" -> rankRule(
                    "official", "官员",
                    3, 4,
                    List.of(
                            color("红色", "#c4473a", "官式建筑常见色"),
                            color("灰色", "#888888", "瓦面与石构常用色"),
                            color("青色", "#2c5a7d", "木作与屋面辅色"),
                            color("白色", "#f2eada", "墙面与灰浆本色")
                    )
            );
            case "wealthy", "富户", "富商" -> rankRule(
                    "wealthy", "富户",
                    2, 3,
                    List.of(
                            color("灰色", "#888888", "常见瓦面本色"),
                            color("棕褐色", "#704d30", "木构与漆色常用色"),
                            color("白色", "#f2eada", "墙面常用色")
                    )
            );
            case "civilian", "平民", "庶民", "" -> rankRule(
                    "civilian", "平民",
                    1, 3,
                    List.of(
                            color("灰色", "#888888", "布瓦本色"),
                            color("青色", "#2c5a7d", "常用建筑用色"),
                            color("黑色", "#333333", "普通木材本色")
                    )
            );
            default -> rankRule(
                    "civilian", "平民",
                    1, 3,
                    List.of(
                            color("灰色", "#888888", "布瓦本色"),
                            color("青色", "#2c5a7d", "常用建筑用色"),
                            color("黑色", "#333333", "普通木材本色")
                    )
            );
        };
    }

    private ColorPrivilege findColorPrivilege(String rawHex) {
        String hex = normalizeHex(rawHex);
        if (Set.of("#ffbb00", "#f3b746", "#c9a227").contains(hex)) {
            return new ColorPrivilege("黄色", 5, "皇家", "历史知识：黄色在明清礼制中高度象征皇权，常与皇家屋顶和核心礼制空间绑定。");
        }
        if (Set.of("#5e9c45", "#4f8f6f").contains(hex)) {
            return new ColorPrivilege("绿色", 4, "王公或高等级官式建筑", "历史知识：绿色多见于较高等级建筑的琉璃瓦和彩画系统，民间通常难以大规模使用。");
        }
        if (Set.of("#4d79a7", "#2c5a7d").contains(hex)) {
            return new ColorPrivilege("蓝青色", 3, "官员及以上建筑", "历史知识：青蓝色常用于木作彩画和屋面辅色，出现时往往与官式建造体系有关。");
        }
        if (Set.of("#c4473a", "#a14436").contains(hex)) {
            return new ColorPrivilege("红色", 3, "官员及以上建筑", "历史知识：朱红在礼制建筑中常用于柱、门、墙等显性部位，等级意味明显。");
        }
        if (Set.of("#704d30", "#7a5536").contains(hex)) {
            return new ColorPrivilege("棕褐色", 2, "富户及以上建筑", "历史知识：棕褐与木构本色、漆色接近，在民居和富户建筑中都较常见。");
        }
        if (Set.of("#888888", "#333333", "#f2eada").contains(hex)) {
            return new ColorPrivilege("灰白黑中性色", 1, "平民建筑", "历史知识：灰、白、黑等中性色是传统民居中最常见的基础色。");
        }
        return new ColorPrivilege("未知颜色", 2, "较低等级建筑", "历史知识：未收录颜色会按接近中低阶常用色处理，建议结合具体部位再判断。");
    }

    private String normalizeHex(String rawHex) {
        if (!StringUtils.hasText(rawHex)) {
            return "";
        }
        String hex = rawHex.trim().toLowerCase(Locale.ROOT);
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        return hex;
    }

    private Map<String, Object> color(String name, String hex, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("hex", hex);
        item.put("description", description);
        return item;
    }

    private RankRule rankRule(String id, String name, int rankOrder, int maxColors, List<Map<String, Object>> colors) {
        return new RankRule(id, name, rankOrder, maxColors, colors);
    }

    private record RankRule(String id, String name, int rankOrder, int maxColors, List<Map<String, Object>> colors) {
    }

    private record ColorPrivilege(String name, int minRankOrder, String reservedFor, String knowledgePoint) {
    }

    private record Violation(String part, String colorHex, ColorPrivilege privilege) {
        private Map<String, Object> toMap() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("part", part);
            item.put("colorHex", colorHex);
            item.put("reservedFor", privilege.reservedFor());
            item.put("colorName", privilege.name());
            return item;
        }
    }
}
