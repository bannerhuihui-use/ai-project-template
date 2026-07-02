package com.example.template.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger 文档配置。
 *
 * <p>用于统一声明文档元信息、服务地址与全局标签，配合 springdoc 生成可在线调试的接口文档。
 * 访问地址（默认）：
 * <ul>
 *     <li>Swagger UI：http://localhost:8080/swagger-ui.html</li>
 *     <li>OpenAPI JSON：http://localhost:8080/v3/api-docs</li>
 *     <li>OpenAPI YAML：http://localhost:8080/v3/api-docs.yaml</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    /** 文档标题。 */
    private static final String API_TITLE = "AI 项目初始化模板 API";

    /** 文档描述。 */
    private static final String API_DESCRIPTION = "用于展示、调试和测试后端接口的 OpenAPI 文档";

    /** 文档版本。 */
    private static final String API_VERSION = "v1.0.0";

    /** 本地服务地址。 */
    private static final String LOCAL_SERVER_URL = "http://localhost:8080";

    /** 本地服务描述。 */
    private static final String LOCAL_SERVER_DESCRIPTION = "本地开发环境";

    /** 示例接口标签名，与 Controller 类级 @Tag 保持一致。 */
    private static final String TAG_EXAMPLE = "示例接口";

    /** 示例接口标签描述。 */
    private static final String TAG_EXAMPLE_DESCRIPTION = "项目模板自带的示例与健康检查接口";

    /** Bearer JWT 安全方案名称，与接口上的 @SecurityRequirement(name=...) 对应。 */
    private static final String SECURITY_SCHEME_BEARER = "bearerAuth";

    @Bean
    public OpenAPI templateOpenAPI() {
        Info info = new Info()
                .title(API_TITLE)
                .description(API_DESCRIPTION)
                .version(API_VERSION)
                .contact(new Contact().name("Template Team"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"));

        Server localServer = new Server()
                .url(LOCAL_SERVER_URL)
                .description(LOCAL_SERVER_DESCRIPTION);

        Tag exampleTag = new Tag()
                .name(TAG_EXAMPLE)
                .description(TAG_EXAMPLE_DESCRIPTION);

        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("在此填入登录接口返回的 accessToken，请求将自动带上 Authorization: Bearer <token>");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .tags(List.of(exampleTag))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_BEARER, bearerScheme));
    }
}
