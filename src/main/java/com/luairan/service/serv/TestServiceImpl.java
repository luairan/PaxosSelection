package com.luairan.service.serv;

import org.springframework.stereotype.Service;

/**
 * Created by luairan on 16/9/5.
 */

@Service
public class TestServiceImpl implements TestService {
    @Override
    public String getName() {
        return "luairan";
    }
}
