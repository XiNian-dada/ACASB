package com.leeinx.acasb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

//ACASB Ancient Chinese Architecture in Spring Boot

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AcasbApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcasbApplication.class, args);
    }
}