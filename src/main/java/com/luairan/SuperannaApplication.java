package com.luairan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@EnableAutoConfiguration
//@SpringBootApplication
@Configuration
@ComponentScan
public class SuperannaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuperannaApplication.class, args);
    }
}
