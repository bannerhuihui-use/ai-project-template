package com.example.template.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新系统配置元数据请求（不含 configKey 与 configValue）。
 */
@Schema(name = "ConfigMetaUpdateRequest", description = "更新系统配置元数据")
public class ConfigMetaUpdateRequest {

    @Schema(description = "配置名称", example = "通知开关", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 128, message = "配置名称最长 128 字符")
    private String configName;

    @Schema(description = "配置分组", example = "custom", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置分组不能为空")
    @Size(max = 64, message = "配置分组最长 64 字符")
    @Pattern(regexp = "^[a-z][a-z0-9_-]{0,63}$", message = "分组仅支持小写字母、数字、下划线、连字符")
    private String configGroup;

    @Schema(description = "说明")
    @Size(max = 512, message = "说明最长 512 字符")
    private String description;

    @Schema(description = "排序号", example = "100")
    @Min(value = 0, message = "排序号不能小于 0")
    @Max(value = 9999, message = "排序号不能大于 9999")
    private Integer sortNo;

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
