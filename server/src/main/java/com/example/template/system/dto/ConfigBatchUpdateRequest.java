package com.example.template.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量更新系统配置请求。
 */
@Schema(description = "批量更新系统配置请求")
public class ConfigBatchUpdateRequest {

    @Schema(description = "待更新配置列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "更新列表不能为空")
    @Size(max = 100, message = "单次最多更新 100 项配置")
    @Valid
    private List<ConfigUpdateItem> items;

    public List<ConfigUpdateItem> getItems() {
        return items;
    }

    public void setItems(List<ConfigUpdateItem> items) {
        this.items = items;
    }
}
