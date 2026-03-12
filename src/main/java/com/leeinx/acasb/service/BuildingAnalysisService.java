package com.leeinx.acasb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leeinx.acasb.entity.BuildingAnalysis;
import com.leeinx.acasb.mapper.BuildingAnalysisMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

@Service
public class BuildingAnalysisService extends ServiceImpl<BuildingAnalysisMapper, BuildingAnalysis> {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "ratio_yellow",
            "ratio_red_1",
            "ratio_red_2",
            "ratio_blue",
            "ratio_green",
            "ratio_gray_white",
            "ratio_black",
            "h_mean",
            "h_std",
            "s_mean",
            "s_std",
            "v_mean",
            "v_std",
            "edge_density",
            "entropy",
            "contrast",
            "dissimilarity",
            "homogeneity",
            "asm",
            "royal_ratio",
            "create_time",
            "update_time"
    );
    
    public BuildingAnalysis saveAnalysis(BuildingAnalysis analysis) {
        analysis.setCreateTime(LocalDateTime.now());
        analysis.setUpdateTime(LocalDateTime.now());
        save(analysis);
        return analysis;
    }
    
    public BuildingAnalysis getAnalysisById(Long id) {
        return getById(id);
    }
    
    public List<BuildingAnalysis> getAnalysesByField(String field, String order, Integer limit) {
        String dbField = camelToSnake(field);
        if (!ALLOWED_SORT_FIELDS.contains(dbField)) {
            dbField = "royal_ratio";
        }

        String normalizedOrder = "asc".equalsIgnoreCase(order) ? "asc" : "desc";
        int normalizedLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 200);

        return baseMapper.selectByFieldAndOrder(dbField, normalizedOrder, normalizedLimit);
    }
    
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
