package com.example.template.auth.controller;

import com.example.template.auth.dto.AdminLoginRequest;
import com.example.template.auth.dto.AuthUserInfo;
import com.example.template.auth.dto.ChangePasswordRequest;
import com.example.template.auth.dto.LoginContext;
import com.example.template.auth.dto.LoginResponse;
import com.example.template.auth.dto.LogoutRequest;
import com.example.template.auth.dto.RefreshTokenRequest;
import com.example.template.auth.dto.WechatLoginRequest;
import com.example.template.auth.service.AuthService;
import com.example.template.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权接口。Controller 仅做参数接收/校验（含客户端信息提取），业务逻辑在 {@link AuthService}。
 */
@Tag(name = "鉴权", description = "登录、刷新令牌、当前用户、退出登录等鉴权接口")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /** User-Agent 入库最大长度，防止超长写入。 */
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "后台账号密码登录",
            description = "校验用户名密码并签发 access + refresh 双令牌。仅 user_type=ADMIN 且状态正常的账号可登录。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功，返回双令牌与用户信息",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(value = "{\n  \"code\": 0,\n  \"message\": \"success\",\n"
                                    + "  \"data\": {\n    \"accessToken\": \"eyJ...\",\n"
                                    + "    \"refreshToken\": \"eyJ...\",\n    \"tokenType\": \"Bearer\",\n"
                                    + "    \"accessExpireSeconds\": 7200,\n    \"refreshExpireSeconds\": 604800,\n"
                                    + "    \"user\": {\"userId\": 1, \"userType\": \"ADMIN\", "
                                    + "\"nickname\": \"管理员\", \"roles\": [\"ADMIN\"]}\n  }\n}"))),
            @ApiResponse(responseCode = "200", description = "失败：40101 账号不存在或密码错误；"
                    + "40102 账号已被禁用；40301 非管理员账号",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(value = "{\"code\": 40101, "
                                    + "\"message\": \"用户名或密码错误\", \"data\": null}")))
    })
    @PostMapping("/admin/login")
    public Result<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request,
                                            HttpServletRequest httpRequest) {
        return Result.success(authService.adminLogin(request, buildLoginContext(httpRequest)));
    }

    @Operation(summary = "微信小程序登录（v1 占位）",
            description = "结构与文档先行，v1 暂未接入 wechat-miniapp，调用将返回业务错误 50010。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "失败：50010 微信登录能力待接入 wechat-miniapp 模块",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(value = "{\"code\": 50010, "
                                    + "\"message\": \"微信登录能力待接入 wechat-miniapp 模块\", \"data\": null}")))
    })
    @PostMapping("/wechat/login")
    public Result<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return Result.success(authService.wechatLogin(request));
    }

    @Operation(summary = "刷新令牌",
            description = "用 refresh token 换取新的 access + refresh 令牌；旧 refresh 会被拉黑（轮换防重放）。"
                    + "无需携带 Authorization 头。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刷新成功，返回新的双令牌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(value = "{\n  \"code\": 0,\n  \"message\": \"success\",\n"
                                    + "  \"data\": {\n    \"accessToken\": \"eyJ...\",\n"
                                    + "    \"refreshToken\": \"eyJ...\",\n    \"tokenType\": \"Bearer\",\n"
                                    + "    \"accessExpireSeconds\": 7200,\n    \"refreshExpireSeconds\": 604800,\n"
                                    + "    \"user\": {\"userId\": 1, \"userType\": \"ADMIN\", \"roles\": [\"ADMIN\"]}\n"
                                    + "  }\n}"))),
            @ApiResponse(responseCode = "200", description = "失败：40104 刷新令牌无效/过期；40105 已失效；"
                    + "40106 令牌类型不正确；40107 登录已失效；50301 鉴权服务暂不可用",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(value = "{\"code\": 40104, "
                                    + "\"message\": \"刷新令牌无效或已过期，请重新登录\", \"data\": null}")))
    })
    @PostMapping("/token/refresh")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refreshToken(request));
    }

    @Operation(summary = "获取当前登录用户", description = "需携带有效 access token，返回当前用户基本信息。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功，返回当前用户信息",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未认证或 Token 无效（40100 / 40103 / 40105 / 40106 / 40107）",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class)))
    })
    @GetMapping("/me")
    public Result<AuthUserInfo> me() {
        return Result.success(authService.currentUser());
    }

    @Operation(summary = "修改密码",
            description = "校验原密码后更新为新密码，并使该用户此前所有令牌失效；成功后返回续签的新 access + refresh。"
                    + "仅修改当前登录用户自己的密码。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功，返回续签的新双令牌",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40108 原密码错误；40109 新密码与原密码相同；"
                    + "40000 新密码不合规（如长度不足）",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(value = "{\"code\": 40108, \"message\": \"原密码错误\", \"data\": null}"))),
            @ApiResponse(responseCode = "401", description = "未认证或令牌失效")
    })
    @PostMapping("/password/change")
    public Result<LoginResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return Result.success(authService.changePassword(request));
    }

    @Operation(summary = "退出登录",
            description = "拉黑当前 access token；若请求体携带 refreshToken 则一并拉黑。退出后两者均不可再用。"
                    + "黑名单关闭时回退为整账号失效（token_version 自增）。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "退出成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request);
        return Result.success();
    }

    @Operation(summary = "管理员示例受保护接口", description = "仅 ROLE_ADMIN 可访问，用于验证鉴权保护链路。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未认证或 Token 无效"),
            @ApiResponse(responseCode = "403", description = "已认证但非管理员（40300）")
    })
    @GetMapping("/admin/ping")
    public Result<String> adminPing() {
        return Result.success("pong");
    }

    /**
     * 从请求提取客户端信息（IP 取 remoteAddr 避免伪造；UA 截断）。
     *
     * @param request HTTP 请求
     * @return 登录上下文
     */
    private LoginContext buildLoginContext(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (StringUtils.hasText(userAgent) && userAgent.length() > MAX_USER_AGENT_LENGTH) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
        }
        return new LoginContext(ip, userAgent);
    }
}
