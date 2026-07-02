package com.example.template.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 单条配置更新项。
 */
@Schema(description = "配置更新项")
public class ConfigUpdateItem {

    @Schema(description = "配置键", example = "site.name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
