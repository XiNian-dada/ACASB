package com.leeinx.acasb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leeinx.acasb.entity.BuildingAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BuildingAnalysisMapper extends BaseMapper<BuildingAnalysis> {
    
    @Select("SELECT * FROM building_analysis ORDER BY ${field} ${order} LIMIT ${limit}")
    List<BuildingAnalysis> selectByFieldAndOrder(
            @Param("field") String field,
            @Param("order") String order,
            @Param("limit") Integer limit);
}
