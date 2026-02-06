package com.leeinx.acasb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leeinx.acasb.entity.BuildingAnalysis;
import com.leeinx.acasb.mapper.BuildingAnalysisMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BuildingAnalysisService extends ServiceImpl<BuildingAnalysisMapper, BuildingAnalysis> {
    
    public BuildingAnalysis saveAnalysis(BuildingAnalysis analysis) {
        analysis.setCreateTime(LocalDateTime.now());
        analysis.setUpdateTime(LocalDateTime.now());
        save(analysis);
        return analysis;
    }
    
    public BuildingAnalysis getLatestAnalysis(String imagePath) {
        return getOne(new LambdaQueryWrapper<BuildingAnalysis>()
                .eq(BuildingAnalysis::getImagePath, imagePath)
                .orderByDesc(BuildingAnalysis::getCreateTime)
                .last("LIMIT 1"));
    }
    
    public BuildingAnalysis getAnalysisById(Long id) {
        return getById(id);
    }
}
