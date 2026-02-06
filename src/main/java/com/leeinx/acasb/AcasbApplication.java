package com.leeinx.acasb;

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
    public static void main(String[] args) {
        SpringApplication.run(AcasbApplication.class, args);
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void run(String... args) throws Exception {
        String token = JwtUtils.createTimestampToken();
        System.out.println("\n\n");
        System.out.println("==================================================================");
        System.out.println("🏛️  ACASB - 后端服务启动成功");
        System.out.println("🔑 本次实例 Token (重启失效): ");
        System.out.println("------------------------------------------------------------------");
        System.out.println(token);
        System.out.println("------------------------------------------------------------------");
        System.out.println("⚠️  请在 Postman 或前端 Header 中添加: Authorization: Bearer <Token>");
        System.out.println("==================================================================\n\n");
    }
}