package com.example.template.controller;

import com.example.template.common.Result;
import com.example.template.service.HelloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 示例接口。Controller 只负责参数接收/校验与结果返回，业务逻辑委托给 {@link HelloService}。
 */
@Tag(name = "示例接口", description = "项目模板自带的示例与健康检查接口")
@RestController
@RequestMapping("/api/v1")
public class HelloController {

    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @Operation(
            summary = "问候接口",
            description = "根据传入的 name 返回一句问候语，用于验证服务连通性与 Swagger 调试。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "调用成功，data 为问候语字符串",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(
                                    name = "成功示例",
                                    value = "{\n  \"code\": 0,\n  \"message\": \"success\",\n  \"data\": \"hello, world\"\n}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "服务端异常（错误码 50001）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(
                                    name = "系统异常示例",
                                    value = "{\n  \"code\": 50001,\n  \"message\": \"系统异常\",\n  \"data\": null\n}"
                            )
                    )
            )
    })
    @GetMapping("/hello")
    public Result<String> hello(
            @Parameter(
                    description = "被问候者名称，可选；不传时默认 world",
                    required = false,
                    example = "Alice",
                    schema = @Schema(type = "string", defaultValue = "world")
            )
            @RequestParam(defaultValue = "world") String name) {
        return Result.success(helloService.greet(name));
    }

    @Operation(
            summary = "健康检查接口",
            description = "返回服务运行信息，data 为对象，包含 app（应用名）、profile（当前环境）、"
                    + "status（运行状态，固定 UP）、time（服务器当前时间）。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "调用成功，data 为服务运行信息对象",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(
                                    name = "成功示例",
                                    value = "{\n  \"code\": 0,\n  \"message\": \"success\",\n  \"data\": {\n"
                                            + "    \"app\": \"template-server\",\n"
                                            + "    \"profile\": \"local\",\n"
                                            + "    \"status\": \"UP\",\n"
                                            + "    \"time\": \"2026-06-30 11:19:00\"\n  }\n}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "服务端异常（错误码 50001）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(
                                    name = "系统异常示例",
                                    value = "{\n  \"code\": 50001,\n  \"message\": \"系统异常\",\n  \"data\": null\n}"
                            )
                    )
            )
    })
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(helloService.serverInfo());
    }
}
