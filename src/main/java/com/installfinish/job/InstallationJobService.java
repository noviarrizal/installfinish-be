package com.installfinish.job;

import com.installfinish.audit.AuditAction;
import com.installfinish.audit.AuditService;
import com.installfinish.client.ClientAccount;
import com.installfinish.client.ClientAccountService;
import com.installfinish.closeout.CloseoutCaseService;
import com.installfinish.closeout.CloseoutCaseSummary;
import com.installfinish.common.api.PageResponse;
import com.installfinish.common.error.ConflictException;
import com.installfinish.common.error.ResourceNotFoundException;
import com.installfinish.job.CreateInstallationJobRequest.ServiceLocationRequest;
import com.installfinish.security.Actor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstallationJobService {

    private final InstallationJobRepository installationJobRepository;
    private final ServiceLocationRepository serviceLocationRepository;
    private final ClientAccountService clientAccountService;
    private final CloseoutCaseService closeoutCaseService;
    private final AuditService auditService;

    public InstallationJobService(InstallationJobRepository installationJobRepository,
                                  ServiceLocationRepository serviceLocationRepository,
                                  ClientAccountService clientAccountService,
                                  CloseoutCaseService closeoutCaseService,
                                  AuditService auditService) {
        this.installationJobRepository = installationJobRepository;
        this.serviceLocationRepository = serviceLocationRepository;
        this.clientAccountService = clientAccountService;
        this.closeoutCaseService = closeoutCaseService;
        this.auditService = auditService;
    }

    /** Job intake: records the job and its site, and opens the closeout case, in one transaction. */
    @Transactional
    public InstallationJobResponse create(Actor actor, CreateInstallationJobRequest request) {
        ClientAccount account = clientAccountService.requireActiveAccount(actor, request.clientAccountId());
        String externalJobId = trimToNull(request.externalJobId());
        if (externalJobId != null
                && installationJobRepository.existsByClientAccountIdAndExternalJobId(account.getId(), externalJobId)) {
            throw new ConflictException("DUPLICATE_EXTERNAL_JOB_ID",
                    "A job with external id '" + externalJobId + "' already exists for this client account.");
        }

        ServiceLocation location = serviceLocationRepository.save(toServiceLocation(account.getId(),
                request.serviceLocation()));
        InstallationJob job = new InstallationJob(account.getId(), location, externalJobId, request.jobType(),
                Objects.requireNonNullElse(request.intakeChannel(), IntakeChannel.MANUAL),
                trimToNull(request.sourceSystem()), actor.subject());
        job.recordHomeowner(trimToNull(request.homeownerName()), trimToNull(request.homeownerEmail()),
                trimToNull(request.homeownerPhone()));
        job.recordInstallation(request.soldDate(), request.installedDate(), trimToNull(request.contractorTeam()),
                Objects.requireNonNullElse(request.equipment(), List.of()));
        job = installationJobRepository.save(job);

        auditService.record(actor, account.getId(), AuditAction.INSTALLATION_JOB_CREATED, job.getId(),
                auditSnapshot(job));
        CloseoutCaseSummary closeoutCase = closeoutCaseService.openCaseForJob(actor, account.getId(), job.getId());

        return InstallationJobResponse.from(job, closeoutCase);
    }

    @Transactional(readOnly = true)
    public InstallationJobResponse get(Actor actor, UUID jobId) {
        InstallationJob job = actor.clientScope()
                .map(clientAccountId ->
                        installationJobRepository.findWithLocationByIdAndClientAccountId(jobId, clientAccountId))
                .orElseGet(() -> installationJobRepository.findWithLocationById(jobId))
                .orElseThrow(() -> new ResourceNotFoundException("Installation job", jobId));
        CloseoutCaseSummary closeoutCase = closeoutCaseService.summariesByJobId(actor, List.of(job.getId()))
                .get(job.getId());
        return InstallationJobResponse.from(job, closeoutCase);
    }

    @Transactional(readOnly = true)
    public PageResponse<InstallationJobResponse> list(Actor actor, UUID clientAccountId, Pageable pageable) {
        Page<InstallationJob> jobs = actor.resolveClientFilter(clientAccountId)
                .map(scope -> installationJobRepository.findByClientAccountId(scope, pageable))
                .orElseGet(() -> installationJobRepository.findAllBy(pageable));
        Map<UUID, CloseoutCaseSummary> cases = closeoutCaseService.summariesByJobId(actor,
                jobs.getContent().stream().map(InstallationJob::getId).toList());
        return PageResponse.from(jobs, job -> InstallationJobResponse.from(job, cases.get(job.getId())));
    }

    private static ServiceLocation toServiceLocation(UUID clientAccountId, ServiceLocationRequest request) {
        return new ServiceLocation(clientAccountId, request.addressLine1().strip(),
                trimToNull(request.addressLine2()), request.city().strip(), request.stateCode(),
                request.postalCode(), trimToNull(request.county()));
    }

    /** Homeowner contact details and serial numbers are deliberately excluded: audit events are never deleted. */
    private static Map<String, Object> auditSnapshot(InstallationJob job) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("externalJobId", job.getExternalJobId());
        snapshot.put("jobType", job.getJobType());
        snapshot.put("intakeChannel", job.getIntakeChannel());
        snapshot.put("sourceSystem", job.getSourceSystem());
        snapshot.put("serviceLocationId", job.getServiceLocation().getId());
        snapshot.put("equipmentCount", job.getEquipment().size());
        return snapshot;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
