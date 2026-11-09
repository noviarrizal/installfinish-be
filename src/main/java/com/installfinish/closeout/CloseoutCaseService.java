package com.installfinish.closeout;

import com.installfinish.audit.AuditAction;
import com.installfinish.audit.AuditService;
import com.installfinish.common.api.PageResponse;
import com.installfinish.common.error.ResourceNotFoundException;
import com.installfinish.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseoutCaseService {

    private final CloseoutCaseRepository closeoutCaseRepository;
    private final AuditService auditService;
    private final Clock clock;

    public CloseoutCaseService(CloseoutCaseRepository closeoutCaseRepository, AuditService auditService, Clock clock) {
        this.closeoutCaseRepository = closeoutCaseRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Opens the closeout case for a newly received job. Must run inside the transaction that created the job; the
     * caller is responsible for having verified the actor's access to the client account.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CloseoutCaseSummary openCaseForJob(Actor actor, UUID clientAccountId, UUID installationJobId) {
        CloseoutCase closeoutCase = closeoutCaseRepository.saveAndFlush(
                CloseoutCase.received(clientAccountId, installationJobId, Instant.now(clock)));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("caseReference", closeoutCase.getCaseReference());
        snapshot.put("installationJobId", installationJobId);
        snapshot.put("status", closeoutCase.getStatus());
        auditService.record(actor, clientAccountId, AuditAction.CLOSEOUT_CASE_OPENED, closeoutCase.getId(), snapshot);

        return CloseoutCaseSummary.from(closeoutCase);
    }

    @Transactional(readOnly = true)
    public CloseoutCaseResponse get(Actor actor, UUID caseId) {
        CloseoutCase closeoutCase = actor.clientScope()
                .map(clientAccountId -> closeoutCaseRepository.findByIdAndClientAccountId(caseId, clientAccountId))
                .orElseGet(() -> closeoutCaseRepository.findById(caseId))
                .orElseThrow(() -> new ResourceNotFoundException("Closeout case", caseId));
        return CloseoutCaseResponse.from(closeoutCase);
    }

    @Transactional(readOnly = true)
    public PageResponse<CloseoutCaseResponse> search(Actor actor, UUID clientAccountId, CloseoutCaseStatus status,
                                                     Pageable pageable) {
        List<Specification<CloseoutCase>> filters = new ArrayList<>();
        actor.resolveClientFilter(clientAccountId).ifPresent(scope ->
                filters.add((root, query, cb) -> cb.equal(root.get("clientAccountId"), scope)));
        if (status != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return PageResponse.from(closeoutCaseRepository.findAll(Specification.allOf(filters), pageable),
                CloseoutCaseResponse::from);
    }

    /** Case summaries keyed by installation job id, limited to cases the actor may see. */
    @Transactional(readOnly = true)
    public Map<UUID, CloseoutCaseSummary> summariesByJobId(Actor actor, Collection<UUID> installationJobIds) {
        if (installationJobIds.isEmpty()) {
            return Map.of();
        }
        return closeoutCaseRepository.findByInstallationJobIdIn(installationJobIds).stream()
                .filter(closeoutCase -> actor.canAccessClient(closeoutCase.getClientAccountId()))
                .collect(Collectors.toMap(CloseoutCase::getInstallationJobId, CloseoutCaseSummary::from));
    }
}
