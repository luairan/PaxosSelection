package com.luairan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@RequestMapping("/user")
public class SuperannaApplication {

    public static void main(String[] args) {
            SpringApplication.run(SuperannaApplication.class, args);
    }

    @RequestMapping(value = "/new.do", method = RequestMethod.GET)
    public String getNewForm() {
        return "luairan";
    }

}
