package com.example.template.service.impl;

import com.example.template.service.HelloService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 示例业务实现（业务层）。
 */
@Service
public class HelloServiceImpl implements HelloService {

    @Value("${spring.application.name:template-server}")
    private String appName;

    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;

    @Override
    public String greet(String name) {
        return "hello, " + name;
    }

    @Override
    public Map<String, Object> serverInfo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", appName);
        data.put("profile", activeProfile);
        data.put("status", "UP");
        data.put("time", LocalDateTime.now().toString());
        return data;
    }
}
