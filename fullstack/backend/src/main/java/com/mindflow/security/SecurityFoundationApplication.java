package com.mindflow.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecurityFoundationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityFoundationApplication.class, args);
    }
}
