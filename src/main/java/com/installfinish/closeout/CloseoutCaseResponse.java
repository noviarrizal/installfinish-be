package com.installfinish.closeout;

import java.time.Instant;
import java.util.UUID;

public record CloseoutCaseResponse(
        UUID id,
        String caseReference,
        UUID clientAccountId,
        UUID installationJobId,
        CloseoutCaseStatus status,
        String statusReason,
        Instant statusChangedAt,
        CasePriority priority,
        UUID playbookId,
        String assignedOperator,
        Instant dueAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    static CloseoutCaseResponse from(CloseoutCase c) {
        return new CloseoutCaseResponse(c.getId(), c.getCaseReference(), c.getClientAccountId(),
                c.getInstallationJobId(), c.getStatus(), c.getStatusReason(), c.getStatusChangedAt(),
                c.getPriority(), c.getPlaybookId(), c.getAssignedOperator(), c.getDueAt(), c.getClosedAt(),
                c.getCreatedAt(), c.getUpdatedAt(), c.getVersion());
    }
}
