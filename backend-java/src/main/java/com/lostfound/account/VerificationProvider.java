package com.lostfound.account;

/**
 * 身份验证提供方抽象。MVP 为校园邮箱核验（EmailProvider），后续可替换为微信「学生验证」等实现。
 */
public interface VerificationProvider {

    /** 发送验证码，返回验证码有效期（秒）。域名不允许时抛 VALIDATION_FAILED。 */
    int send(String email);

    /** 校验验证码是否通过。 */
    boolean confirm(String email, String code);
}
