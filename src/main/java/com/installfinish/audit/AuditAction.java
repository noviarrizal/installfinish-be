package com.installfinish.audit;

/** Auditable actions. The code is persisted, so existing codes must never be renamed. */
public enum AuditAction {

    CLIENT_ACCOUNT_CREATED("client_account.created", "client_account"),
    CLIENT_USER_CREATED("client_user.created", "client_user"),
    INSTALLATION_JOB_CREATED("installation_job.created", "installation_job"),
    CLOSEOUT_CASE_OPENED("closeout_case.opened", "closeout_case");

    private final String code;
    private final String entityType;

    AuditAction(String code, String entityType) {
        this.code = code;
        this.entityType = entityType;
    }

    public String code() {
        return code;
    }

    public String entityType() {
        return entityType;
    }
}
