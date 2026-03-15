package com.leeinx.acasb;

import com.leeinx.acasb.config.AuthProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.leeinx.acasb.jwt.JwtUtils;

//ACASB Ancient Chinese Architecture in Spring Boot

@SpringBootApplication
@MapperScan("com.leeinx.acasb.mapper")
public class AcasbApplication implements CommandLineRunner{
    private final AuthProperties authProperties;

    public AcasbApplication(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public static void main(String[] args) {
        SpringApplication.run(AcasbApplication.class, args);
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void run(String... args) throws Exception {
        boolean jwtEnabled = authProperties.isEnabled();
        String token = JwtUtils.createTimestampToken();
        System.out.println("\n\n");
        System.out.println("==================================================================");
        System.out.println("🏛️  ACASB - 后端服务启动成功");
        System.out.println("🔐 鉴权状态: " + (jwtEnabled ? "已开启" : "已关闭"));
        if (jwtEnabled) {
            System.out.println("🔑 本次实例 Token (重启失效): ");
            System.out.println("------------------------------------------------------------------");
            System.out.println(token);
            System.out.println("------------------------------------------------------------------");
            System.out.println("⚠️  请在 Postman 或前端 Header 中添加: Authorization: Bearer <Token>");
        } else {
            System.out.println("⚠️  当前未启用 JWT 鉴权，请勿直接用于生产环境");
        }
        System.out.println("==================================================================\n\n");
    }
}
