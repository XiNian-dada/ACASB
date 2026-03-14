package com.leeinx.acasb.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        createBuildingAnalysisTable();
        ensureBuildingAnalysisColumns();
        createBuildingTypeTable();
        createDatasetImageRecordTable();
        System.out.println("数据库表初始化完成！");
    }

    private void createBuildingAnalysisTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS building_analysis (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                image_path VARCHAR(500) NOT NULL COMMENT '图片路径',
                ratio_yellow DECIMAL(10,4) COMMENT '黄色比例',
                ratio_red_1 DECIMAL(10,4) COMMENT '红色比例1',
                ratio_red_2 DECIMAL(10,4) COMMENT '红色比例2',
                ratio_blue DECIMAL(10,4) COMMENT '蓝色比例',
                ratio_green DECIMAL(10,4) COMMENT '绿色比例',
                ratio_gray_white DECIMAL(10,4) COMMENT '灰白色比例',
                ratio_black DECIMAL(10,4) COMMENT '黑色比例',
                h_mean DECIMAL(10,4) COMMENT '色相均值',
                h_std DECIMAL(10,4) COMMENT '色相标准差',
                s_mean DECIMAL(10,4) COMMENT '饱和度均值',
                s_std DECIMAL(10,4) COMMENT '饱和度标准差',
                v_mean DECIMAL(10,4) COMMENT '明度均值',
                v_std DECIMAL(10,4) COMMENT '明度标准差',
                edge_density DECIMAL(10,4) COMMENT '边缘密度',
                entropy DECIMAL(10,4) COMMENT '熵值',
                contrast DECIMAL(10,4) COMMENT '对比度',
                dissimilarity DECIMAL(10,4) COMMENT '不相似度',
                homogeneity DECIMAL(10,4) COMMENT '同质性',
                asm DECIMAL(10,4) COMMENT '角二阶矩',
                royal_ratio DECIMAL(10,4) COMMENT '皇家比例',
                ai_analyze LONGTEXT COMMENT '远程 AI 原始解析文本',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='建筑分析信息表'
            """;
        
        try {
            jdbcTemplate.execute(sql);
            System.out.println("building_analysis 表创建成功");
        } catch (Exception e) {
            System.err.println("创建 building_analysis 表失败: " + e.getMessage());
        }
    }

    private void ensureBuildingAnalysisColumns() {
        addColumnIfMissing(
                "building_analysis",
                "ai_analyze",
                "ALTER TABLE building_analysis ADD COLUMN ai_analyze LONGTEXT COMMENT '远程 AI 原始解析文本'"
        );
    }

    private void createBuildingTypeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS building_type (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                image_path VARCHAR(500) NOT NULL COMMENT '图片路径',
                prediction VARCHAR(50) COMMENT '预测结果',
                confidence DECIMAL(10,4) COMMENT '置信度',
                analysis_id BIGINT COMMENT '分析信息ID',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                FOREIGN KEY (analysis_id) REFERENCES building_analysis(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='建筑类型表'
            """;
        
        try {
            jdbcTemplate.execute(sql);
            System.out.println("building_type 表创建成功");
        } catch (Exception e) {
            System.err.println("创建 building_type 表失败: " + e.getMessage());
        }
    }

    private void createDatasetImageRecordTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS dataset_image_record (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                dataset_name VARCHAR(120) NOT NULL COMMENT '数据集名称',
                group_name VARCHAR(120) COMMENT '区域组中文名称',
                group_relative_path VARCHAR(255) COMMENT 'ASCII 规范组路径',
                original_group_relative_path VARCHAR(255) COMMENT '原始组路径',
                schema_version VARCHAR(64) COMMENT 'schema 版本',
                prompt_version VARCHAR(64) COMMENT 'prompt 版本',
                generated_at VARCHAR(64) COMMENT '生成时间',
                api_interface VARCHAR(64) COMMENT '模型接口类型',
                model VARCHAR(120) COMMENT '模型名称',
                province_level_region VARCHAR(120) COMMENT '省级区域',
                province_confidence DECIMAL(10,4) COMMENT '区域置信度',
                dynasty_guess VARCHAR(32) COMMENT '朝代判断',
                building_rank VARCHAR(32) COMMENT '建筑等级',
                scene_type VARCHAR(64) COMMENT '场景类型',
                building_present TINYINT(1) COMMENT '是否存在建筑主体',
                building_primary_colors_json LONGTEXT COMMENT '主体主色列表 JSON',
                building_color_distribution_json LONGTEXT COMMENT '主体色彩占比 JSON',
                architecture_style_json LONGTEXT COMMENT '建筑风格列表 JSON',
                scene_description LONGTEXT COMMENT '场景描述',
                reasoning_json LONGTEXT COMMENT '推理说明 JSON',
                needs_manual_review TINYINT(1) COMMENT '是否需要人工复核',
                file_name VARCHAR(255) COMMENT '文件名',
                relative_path VARCHAR(255) COMMENT 'ASCII 规范相对路径',
                original_relative_path VARCHAR(255) COMMENT '原始相对路径',
                stored_image_path VARCHAR(500) COMMENT '服务端存储路径',
                source_image_path VARCHAR(500) COMMENT '源图片路径',
                analysis_status VARCHAR(32) COMMENT '分析状态',
                error_message VARCHAR(1000) COMMENT '错误信息',
                image_index INT COMMENT '图像序号',
                raw_metadata_json LONGTEXT COMMENT '原始单图元数据 JSON',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                UNIQUE KEY uk_dataset_relative_path (dataset_name, relative_path)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高质量建筑图像标注数据表'
            """;

        try {
            jdbcTemplate.execute(sql);
            System.out.println("dataset_image_record 表创建成功");
        } catch (Exception e) {
            System.err.println("创建 dataset_image_record 表失败: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = ?
                      AND column_name = ?
                    """,
                    Integer.class,
                    tableName,
                    columnName
            );

            if (count != null && count == 0) {
                jdbcTemplate.execute(alterSql);
                System.out.println(tableName + " 表新增字段成功: " + columnName);
            }
        } catch (Exception e) {
            System.err.println("检查或新增字段失败 " + tableName + "." + columnName + ": " + e.getMessage());
        }
    }
}
