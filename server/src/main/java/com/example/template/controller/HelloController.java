package com.example.template.controller;

import com.example.template.common.Result;
import com.example.template.service.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 示例接口。Controller 只负责参数接收/校验与结果返回，业务逻辑委托给 {@link HelloService}。
 */
@RestController
@RequestMapping("/api/v1")
public class HelloController {

    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @GetMapping("/hello")
    public Result<String> hello(@RequestParam(defaultValue = "world") String name) {
        return Result.success(helloService.greet(name));
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(helloService.serverInfo());
    }
}
