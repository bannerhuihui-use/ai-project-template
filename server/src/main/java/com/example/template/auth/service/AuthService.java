package com.example.template.auth.service;

import com.example.template.auth.dto.AdminLoginRequest;
import com.example.template.auth.dto.AuthUserInfo;
import com.example.template.auth.dto.ChangePasswordRequest;
import com.example.template.auth.dto.LoginContext;
import com.example.template.auth.dto.LoginResponse;
import com.example.template.auth.dto.LogoutRequest;
import com.example.template.auth.dto.RefreshTokenRequest;
import com.example.template.auth.dto.WechatLoginRequest;

/**
 * 鉴权业务接口。
 */
public interface AuthService {

    /**
     * 后台账号密码登录，签发 access + refresh 双令牌。
     *
     * @param request 登录请求
     * @param context 登录上下文（IP/UA，用于设备记录）
     * @return 登录响应
     */
    LoginResponse adminLogin(AdminLoginRequest request, LoginContext context);

    /**
     * 微信小程序登录（v1 占位，未接入 wechat-miniapp）。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse wechatLogin(WechatLoginRequest request);

    /**
     * 刷新令牌：校验 refresh token 后轮换签发新的 access + refresh，并拉黑旧 refresh。
     *
     * @param request 刷新请求
     * @return 新的登录响应
     */
    LoginResponse refreshToken(RefreshTokenRequest request);

    /**
     * 修改当前登录用户密码：校验旧密码 → 更新新密码 → token_version 自增（旧令牌全失效），
     * 并续签返回新的 access + refresh（改密即续签）。
     *
     * @param request 改密请求
     * @return 新的登录响应
     */
    LoginResponse changePassword(ChangePasswordRequest request);

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户
     */
    AuthUserInfo currentUser();

    /**
     * 退出登录：拉黑当前 access token（及可选 refresh token）；
     * 黑名单关闭时回退为 token_version 自增（整账号失效）。
     *
     * @param request 退出请求（可选 refreshToken）
     */
    void logout(LogoutRequest request);
}
