package com.luairan.control;

import com.luairan.service.serv.TestService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
public class HelloControl {
    @Resource
    private TestService testService;


    @RequestMapping(value = "/new.do", method = RequestMethod.GET)
    public String getNewForm() {
        return testService.getName();
    }
    
}
