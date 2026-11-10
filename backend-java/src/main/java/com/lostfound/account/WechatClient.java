package com.lostfound.account;

import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 微信 code2session 客户端。
 * 当 wechat.mock=true 时使用确定性桩，便于契约测试与本地开发（契约约定：code=valid-code 返回 openid_test_001）。
 */
@Component
public class WechatClient {

    @Value("${wechat.mock:false}")
    private boolean mock;

    @Value("${wechat.appid:}")
    private String appid;

    @Value("${wechat.secret:}")
    private String secret;

    public String code2sessionOpenid(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.WECHAT_CODE_INVALID);
        }
        if (mock) {
            if ("valid-code".equals(code)) {
                return "openid_test_001";
            }
            if ("valid-code-2".equals(code)) {
                return "openid_test_002";
            }
            if ("valid-code-3".equals(code)) {
                return "openid_test_003";
            }
            throw new BusinessException(ErrorCode.WECHAT_CODE_INVALID);
        }
        return realCode2session(code);
    }

    private String realCode2session(String code) {
        record Resp(String openid, Integer errcode, String errmsg) {
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appid
            + "&secret=" + secret + "&js_code=" + code + "&grant_type=authorization_code";
        Resp resp = RestClient.create().get().uri(url).retrieve().body(Resp.class);
        if (resp == null || resp.openid() == null) {
            throw new BusinessException(ErrorCode.WECHAT_CODE_INVALID);
        }
        return resp.openid();
    }
}
