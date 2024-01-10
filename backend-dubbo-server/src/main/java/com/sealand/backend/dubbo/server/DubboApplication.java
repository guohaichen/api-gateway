package com.sealand.backend.dubbo.server;


import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

/**
 * @author cgh
 * @create 2024-01-02
 * @desc
 */
@SpringBootApplication
@EnableDubbo
public class DubboApplication {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(DubboApplication.class, args);
        System.in.read();
    }
}
