package com.lostfound.account;

public record UserAccount(String id, String openid, String status, String agreementAcceptedAt,
                          String school, String campus) {

    public boolean agreementAccepted() {
        return agreementAcceptedAt != null;
    }
}
