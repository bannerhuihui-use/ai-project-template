package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 登录用户基本信息（登录响应与 /me 复用）。
 */
@Schema(name = "AuthUserInfo", description = "登录用户基本信息")
public class AuthUserInfo {

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "用户类型：ADMIN / MEMBER", example = "ADMIN")
    private String userType;

    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @Schema(description = "角色编码列表", example = "[\"ADMIN\"]")
    private List<String> roles;

    public AuthUserInfo() {
    }

    public AuthUserInfo(Long userId, String userType, String nickname, List<String> roles) {
        this.userId = userId;
        this.userType = userType;
        this.nickname = nickname;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
