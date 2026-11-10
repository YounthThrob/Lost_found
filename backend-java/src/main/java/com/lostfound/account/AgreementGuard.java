package com.lostfound.account;

import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Component;

/** 协议门控：校验用户已确认协议，否则抛出 AGREEMENT_NOT_ACCEPTED。 */
@Component
public class AgreementGuard {

    private final InMemoryUserStore userStore;

    public AgreementGuard(InMemoryUserStore userStore) {
        this.userStore = userStore;
    }

    public UserAccount requireAgreed(String userId) {
        UserAccount user = userStore.findById(userId);
        if (user == null || !user.agreementAccepted()) {
            throw new BusinessException(ErrorCode.AGREEMENT_NOT_ACCEPTED);
        }
        return user;
    }
}
