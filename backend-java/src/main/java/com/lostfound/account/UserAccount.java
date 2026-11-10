package com.lostfound.account;

public record UserAccount(String id, String openid, String status, String agreementAcceptedAt) {

    public boolean agreementAccepted() {
        return agreementAcceptedAt != null;
    }
}
