package com.leeinx.acasb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leeinx.acasb.entity.BuildingType;
import com.leeinx.acasb.mapper.BuildingTypeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BuildingTypeService extends ServiceImpl<BuildingTypeMapper, BuildingType> {
    
    public BuildingType saveType(BuildingType type) {
        type.setCreateTime(LocalDateTime.now());
        type.setUpdateTime(LocalDateTime.now());
        save(type);
        return type;
    }
    
    public BuildingType getLatestType(String imagePath) {
        return getOne(new LambdaQueryWrapper<BuildingType>()
                .eq(BuildingType::getImagePath, imagePath)
                .orderByDesc(BuildingType::getCreateTime)
                .last("LIMIT 1"));
    }
    
    public BuildingType getTypeById(Long id) {
        return getById(id);
    }
}
