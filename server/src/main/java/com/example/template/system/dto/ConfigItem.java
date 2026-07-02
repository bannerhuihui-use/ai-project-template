package com.example.template.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统配置项。
 */
@Schema(description = "系统配置项")
public class ConfigItem {

    @Schema(description = "配置 ID")
    private Long id;

    @Schema(description = "配置键", example = "site.name")
    private String configKey;

    @Schema(description = "配置名称", example = "站点名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置分组", example = "site")
    private String configGroup;

    @Schema(description = "值类型：STRING/NUMBER/BOOLEAN/JSON")
    private String valueType;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "是否可编辑：1 可编辑，0 只读")
    private Integer editable;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "是否为内置配置（种子数据，不可删除）")
    private Boolean builtin;

    @Schema(description = "是否允许删除")
    private Boolean deletable;

    @Schema(description = "是否已接入业务逻辑（改值会生效）")
    private Boolean wired;

    @Schema(description = "已接入时的生效范围说明")
    private String wiredScope;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEditable() {
        return editable;
    }

    public void setEditable(Integer editable) {
        this.editable = editable;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Boolean getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Boolean builtin) {
        this.builtin = builtin;
    }

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public Boolean getWired() {
        return wired;
    }

    public void setWired(Boolean wired) {
        this.wired = wired;
    }

    public String getWiredScope() {
        return wiredScope;
    }

    public void setWiredScope(String wiredScope) {
        this.wiredScope = wiredScope;
    }
}
