package com.installfinish.closeout;

/**
 * Case lifecycle from the product blueprint (section 2.7). Transition rules and evidence requirements are introduced
 * with the case-workflow feature; for now every case starts at {@link #RECEIVED}.
 *
 * <p>Statuses that assert an external fact ({@link #SUBMITTED}, {@link #PERMIT_ISSUED},
 * {@link #WARRANTY_REGISTERED}, ...) must never be set without recorded evidence, and
 * {@link #NO_PERMIT_REQUIRED} only on the basis of a client-approved playbook or written confirmation.
 */
public enum CloseoutCaseStatus {
    RECEIVED,
    DATA_INCOMPLETE,
    READY_FOR_REVIEW,
    CUSTOMER_APPROVAL_PENDING,
    APPROVED_TO_SUBMIT,
    SUBMITTED,
    AUTHORITY_REVIEW,
    CORRECTION_REQUESTED,
    PERMIT_ISSUED,
    NO_PERMIT_REQUIRED,
    FIELD_READY,
    INSPECTION_SCHEDULED,
    INSPECTION_RESULT_RECORDED,
    INSPECTION_FAILED,
    WARRANTY_PENDING,
    WARRANTY_REGISTERED,
    ON_HOLD_CUSTOMER,
    CLOSED,
    CLOSED_WITH_EXCEPTION,
    CANCELLED
}
