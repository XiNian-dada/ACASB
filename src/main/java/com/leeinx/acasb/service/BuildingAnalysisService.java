package com.leeinx.acasb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leeinx.acasb.entity.BuildingAnalysis;
import com.leeinx.acasb.mapper.BuildingAnalysisMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BuildingAnalysisService extends ServiceImpl<BuildingAnalysisMapper, BuildingAnalysis> {
    
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
        return baseMapper.selectByFieldAndOrder(dbField, order, limit);
    }
    
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
