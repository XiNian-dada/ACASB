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
        createBuildingTypeTable();
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
}
