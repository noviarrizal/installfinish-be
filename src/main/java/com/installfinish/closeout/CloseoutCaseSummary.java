package com.installfinish.closeout;

import java.util.UUID;

/** Compact case reference embedded in other resources (e.g. installation jobs). */
public record CloseoutCaseSummary(UUID id, String caseReference, CloseoutCaseStatus status) {

    static CloseoutCaseSummary from(CloseoutCase closeoutCase) {
        return new CloseoutCaseSummary(closeoutCase.getId(), closeoutCase.getCaseReference(), closeoutCase.getStatus());
    }
}
