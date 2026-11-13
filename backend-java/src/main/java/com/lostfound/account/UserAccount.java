package com.lostfound.account;

public record UserAccount(String id, String openid, String status, String agreementAcceptedAt,
                          String school, String campus, String role,
                          String verificationStatus, String verifiedEmail, String emailVerifiedAt) {

    public boolean agreementAccepted() {
        return agreementAcceptedAt != null;
    }

    public boolean isOperator() {
        return "OPERATOR".equals(role);
    }
}
