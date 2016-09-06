package com.luairan.control;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/show")
public class ShowControl {
//    @Resource
//    private TestService testService;

    @RequestMapping(value = "/showMater", method = RequestMethod.GET)
    public String getNewForm() {
        return "31231";
    }
    
}
