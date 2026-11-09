package com.installfinish.security;

/**
 * Roles from the product blueprint (section 2.6). One person may hold several staff roles during the pilot, but
 * permissions stay separated per role.
 */
public enum Role {

    /** Manages users, integrations, permissions and audit. Does not operate cases. */
    SYSTEM_ADMIN(true),
    /** InstallFinish staff who manage cases, tasks, drafts and communication. */
    OPERATOR(true),
    /** InstallFinish staff who check packets and evidence before anything is sent. */
    QUALITY_REVIEWER(true),
    /** Client representative who approves submissions, fees and customer-side decisions. */
    CUSTOMER_APPROVER(false),
    /** Client representative with read-only access to their own account's status and reports. */
    CUSTOMER_VIEWER(false);

    private final boolean staff;

    Role(boolean staff) {
        this.staff = staff;
    }

    public boolean isStaff() {
        return staff;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
