package com.lostfound.operations;

import com.lostfound.account.InMemoryUserStore;
import com.lostfound.account.UserAccount;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Component;

/** 运营鉴权门控：校验当前用户 role = OPERATOR，否则抛出 OPERATOR_REQUIRED。 */
@Component
public class OperatorGuard {

    private final InMemoryUserStore userStore;

    public OperatorGuard(InMemoryUserStore userStore) {
        this.userStore = userStore;
    }

    public UserAccount requireOperator(String userId) {
        UserAccount user = userStore.findById(userId);
        if (user == null || !user.isOperator()) {
            throw new BusinessException(ErrorCode.OPERATOR_REQUIRED);
        }
        return user;
    }
}
