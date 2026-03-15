package com.leeinx.acasb.service;

import com.leeinx.acasb.dto.DatasetColorDistributionItem;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ColorPaletteResolver {
    private static final Map<String, String> EXACT_HEX = new LinkedHashMap<>();

    static {
        EXACT_HEX.put("金色", "#c9a227");
        EXACT_HEX.put("金", "#c9a227");
        EXACT_HEX.put("金黄色", "#d4a62a");
        EXACT_HEX.put("黄色", "#f3b746");
        EXACT_HEX.put("黄", "#f3b746");
        EXACT_HEX.put("土黄色", "#c49a44");
        EXACT_HEX.put("橙黄色", "#d9922e");
        EXACT_HEX.put("橙黄", "#d9922e");
        EXACT_HEX.put("米黄色", "#d8c28a");
        EXACT_HEX.put("赭黄色", "#b98a3d");
        EXACT_HEX.put("黄褐色", "#a67c52");
        EXACT_HEX.put("青绿色", "#4f8f6f");
        EXACT_HEX.put("蓝绿色", "#3f7f78");
        EXACT_HEX.put("灰绿色", "#72866a");
        EXACT_HEX.put("绿色", "#5e9c45");
        EXACT_HEX.put("绿", "#5e9c45");
        EXACT_HEX.put("青色", "#5a7fb4");
        EXACT_HEX.put("青", "#5a7fb4");
        EXACT_HEX.put("青灰色", "#6d7d86");
        EXACT_HEX.put("青蓝色", "#4f6fa3");
        EXACT_HEX.put("蓝色", "#4d79a7");
        EXACT_HEX.put("蓝", "#4d79a7");
        EXACT_HEX.put("灰蓝色", "#6b7c8f");
        EXACT_HEX.put("红色", "#c4473a");
        EXACT_HEX.put("红", "#c4473a");
        EXACT_HEX.put("深红色", "#8f2f2a");
        EXACT_HEX.put("粉红色", "#d98b8b");
        EXACT_HEX.put("赭色", "#8b5a2b");
        EXACT_HEX.put("赭红色", "#9f4a32");
        EXACT_HEX.put("红褐色", "#8b4a32");
        EXACT_HEX.put("棕红色", "#8c4b3e");
        EXACT_HEX.put("红棕色", "#8c5843");
        EXACT_HEX.put("褐红色", "#7d3d31");
        EXACT_HEX.put("棕色", "#7a5536");
        EXACT_HEX.put("棕", "#7a5536");
        EXACT_HEX.put("褐色", "#704d30");
        EXACT_HEX.put("褐", "#704d30");
        EXACT_HEX.put("深棕色", "#5c3c24");
        EXACT_HEX.put("深褐色", "#563928");
        EXACT_HEX.put("棕褐色", "#72533c");
        EXACT_HEX.put("灰褐色", "#7b7064");
        EXACT_HEX.put("木色", "#9a6a3a");
        EXACT_HEX.put("白色", "#f2eada");
        EXACT_HEX.put("白", "#f2eada");
        EXACT_HEX.put("米白色", "#efe5cf");
        EXACT_HEX.put("灰白色", "#ddd6c8");
        EXACT_HEX.put("米色", "#e0d2b4");
        EXACT_HEX.put("灰色", "#888888");
        EXACT_HEX.put("灰", "#888888");
        EXACT_HEX.put("浅灰色", "#b7b7b7");
        EXACT_HEX.put("深灰色", "#5f5f5f");
        EXACT_HEX.put("灰黑色", "#4a4a4a");
        EXACT_HEX.put("黑色", "#333333");
        EXACT_HEX.put("黑", "#333333");
        EXACT_HEX.put("橙色", "#d67a2c");
    }

    private ColorPaletteResolver() {
    }

    public static ColorProfile resolve(String colorName) {
        String original = colorName == null ? "" : colorName.trim();
        String normalized = original.replace(" ", "");
        String hex = EXACT_HEX.get(normalized);
        if (hex == null) {
            hex = inferHex(normalized);
        }
        return new ColorProfile(hex, inferFamily(normalized));
    }

    public static void enrichDistributionItem(DatasetColorDistributionItem item) {
        if (item == null || !StringUtils.hasText(item.getColor())) {
            return;
        }
        item.setHex(resolve(item.getColor()).hex());
    }

    public static void enrichDistributionItems(List<DatasetColorDistributionItem> items) {
        if (items == null) {
            return;
        }
        items.forEach(ColorPaletteResolver::enrichDistributionItem);
    }

    private static String inferHex(String colorName) {
        String lower = colorName == null ? "" : colorName.toLowerCase(Locale.ROOT);
        if (lower.contains("金")) {
            return "#c9a227";
        }
        if (lower.contains("橙")) {
            return "#d67a2c";
        }
        if (lower.contains("黄")) {
            return "#f3b746";
        }
        if (lower.contains("青绿")) {
            return "#4f8f6f";
        }
        if (lower.contains("蓝绿")) {
            return "#3f7f78";
        }
        if (lower.contains("青蓝")) {
            return "#4f6fa3";
        }
        if (lower.contains("青")) {
            return "#5a7fb4";
        }
        if (lower.contains("蓝")) {
            return "#4d79a7";
        }
        if (lower.contains("绿")) {
            return "#5e9c45";
        }
        if (lower.contains("粉")) {
            return "#d98b8b";
        }
        if (lower.contains("红")) {
            return "#c4473a";
        }
        if (lower.contains("棕") || lower.contains("褐") || lower.contains("木") || lower.contains("赭")) {
            return "#7a5536";
        }
        if (lower.contains("米白")) {
            return "#efe5cf";
        }
        if (lower.contains("白")) {
            return "#f2eada";
        }
        if (lower.contains("灰黑")) {
            return "#4a4a4a";
        }
        if (lower.contains("黑")) {
            return "#333333";
        }
        if (lower.contains("浅灰")) {
            return "#b7b7b7";
        }
        if (lower.contains("深灰")) {
            return "#5f5f5f";
        }
        if (lower.contains("灰")) {
            return "#888888";
        }
        return "#999999";
    }

    private static String inferFamily(String colorName) {
        String lower = colorName == null ? "" : colorName.toLowerCase(Locale.ROOT);
        if (lower.contains("金")) {
            return "gold";
        }
        if (lower.contains("黄")) {
            return "yellow";
        }
        if (lower.contains("青绿") || lower.contains("蓝绿")) {
            return "cyan";
        }
        if (lower.contains("青")) {
            return "cyan";
        }
        if (lower.contains("蓝")) {
            return "blue";
        }
        if (lower.contains("绿")) {
            return "green";
        }
        if (lower.contains("橙")) {
            return "orange";
        }
        if (lower.contains("粉") || lower.contains("红")) {
            return "red";
        }
        if (lower.contains("棕") || lower.contains("褐") || lower.contains("木") || lower.contains("赭")) {
            return "brown";
        }
        if (lower.contains("米白") || lower.contains("灰白") || lower.equals("米色") || lower.contains("白")) {
            return "white";
        }
        if (lower.contains("灰黑") || lower.contains("黑")) {
            return "black";
        }
        if (lower.contains("灰")) {
            return "gray";
        }
        return "neutral";
    }

    public record ColorProfile(String hex, String family) {
        public double saturation() {
            return round2(toHsv()[1] * 100.0);
        }

        public double brightness() {
            return round2(toHsv()[2] * 100.0);
        }

        private double[] toHsv() {
            int red = Integer.parseInt(hex.substring(1, 3), 16);
            int green = Integer.parseInt(hex.substring(3, 5), 16);
            int blue = Integer.parseInt(hex.substring(5, 7), 16);

            double r = red / 255.0;
            double g = green / 255.0;
            double b = blue / 255.0;
            double max = Math.max(r, Math.max(g, b));
            double min = Math.min(r, Math.min(g, b));
            double delta = max - min;
            double saturation = max == 0 ? 0 : delta / max;
            return new double[]{0, saturation, max};
        }

        private double round2(double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }
}
