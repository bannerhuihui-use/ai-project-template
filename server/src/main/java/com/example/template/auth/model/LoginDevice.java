package com.example.template.auth.model;

/**
 * 登录设备记录（对应表 auth_login_device）。
 */
public class LoginDevice {

    private Long userId;
    private String identityType;
    private String deviceId;
    private String deviceName;
    private String platform;
    private String ip;
    private String userAgent;

    public LoginDevice(Long userId, String identityType, String deviceId, String deviceName,
                       String platform, String ip, String userAgent) {
        this.userId = userId;
        this.identityType = identityType;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public Long getUserId() {
        return userId;
    }

    public String getIdentityType() {
        return identityType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
