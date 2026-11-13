package com.lostfound.account;

public record UserAccount(String id, String openid, String status, String agreementAcceptedAt,
                          String schoolId, String campusId, String role,
                          String verificationStatus, String verifiedEmail, String emailVerifiedAt,
                          String nickname, String avatarKey) {

    public boolean agreementAccepted() {
        return agreementAcceptedAt != null;
    }

    public boolean isOperator() {
        return "OPERATOR".equals(role);
    }
}
