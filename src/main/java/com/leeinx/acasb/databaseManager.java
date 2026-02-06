package com.leeinx.acasb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class databaseManager {

    
    private static final String URL = "jdbc:mysql://127.0.0.1:2881/your_database?useSSL=false&serverTimezone=UTC&characterEncoding=utf-8";
    
    // OceanBase 用户名格式通常为: 用户名@租户名#集群名 (直连) 或 用户名@租户名 (通过 ODP/OBProxy)
    private static final String USER = "root@test"; 
    private static final String PASSWORD = "your_password";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * 初始化数据库表结构
     * 表名: building_info
     */
    public static void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS building_info (" +
                     "id VARCHAR(64) NOT NULL COMMENT '建筑编号', " +
                     "type VARCHAR(100) COMMENT '建筑类型', " +
                     "analysis_json JSON COMMENT '建筑分析信息(JSON格式)', " +
                     "image_path VARCHAR(512) COMMENT '建筑图片路径', " +
                     "description TEXT COMMENT '建筑简介', " +
                     "PRIMARY KEY (id)" +
                     ") DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("数据表 'building_info' 初始化成功 (或已存在)。");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getBuildingDataById(String target){


    }
}
