package com.luairan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@EnableAutoConfiguration
//@SpringBootApplication
@Configuration
@ComponentScan
public class SuperannaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuperannaApplication.class, args);
    }
}
