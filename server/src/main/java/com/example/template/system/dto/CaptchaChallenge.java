package com.example.template.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录验证码挑战（算术题）。
 */
@Schema(name = "CaptchaChallenge", description = "登录验证码")
public class CaptchaChallenge {

    @Schema(description = "验证码 ID，登录时回传", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String captchaId;

    @Schema(description = "算术题题干", example = "7 + 3 = ?")
    private String question;

    public CaptchaChallenge() {
    }

    public CaptchaChallenge(String captchaId, String question) {
        this.captchaId = captchaId;
        this.question = question;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
