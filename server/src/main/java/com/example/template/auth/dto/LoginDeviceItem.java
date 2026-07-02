package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户登录设备记录（管理端只读）。
 */
@Schema(name = "LoginDeviceItem", description = "登录设备记录")
public class LoginDeviceItem {

    @Schema(description = "记录 ID", example = "1")
    private Long id;

    @Schema(description = "登录身份类型", example = "USERNAME")
    private String identityType;

    @Schema(description = "设备 ID（前端可选传入）", example = "web-chrome-abc")
    private String deviceId;

    @Schema(description = "设备名称", example = "Chrome / macOS")
    private String deviceName;

    @Schema(description = "平台", example = "WEB")
    private String platform;

    @Schema(description = "登录 IP", example = "192.168.1.10")
    private String ip;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}
