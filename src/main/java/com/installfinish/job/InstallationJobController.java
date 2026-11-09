package com.installfinish.job;

import com.installfinish.common.api.PageResponse;
import com.installfinish.security.Actor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Installation jobs")
public class InstallationJobController {

    private static final String READ_ROLES =
            "hasAnyRole('OPERATOR', 'QUALITY_REVIEWER', 'CUSTOMER_APPROVER', 'CUSTOMER_VIEWER')";

    private final InstallationJobService installationJobService;

    public InstallationJobController(InstallationJobService installationJobService) {
        this.installationJobService = installationJobService;
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "Receive an installation job",
            description = "Creates the job and its service location and opens a closeout case in status RECEIVED.")
    public ResponseEntity<InstallationJobResponse> create(@AuthenticationPrincipal Actor actor,
                                                          @Valid @RequestBody CreateInstallationJobRequest request) {
        InstallationJobResponse created = installationJobService.create(actor, request);
        return ResponseEntity.created(URI.create("/api/v1/jobs/" + created.id())).body(created);
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List installation jobs", description = "Client users only ever see their own account's jobs.")
    public PageResponse<InstallationJobResponse> list(
            @AuthenticationPrincipal Actor actor,
            @Parameter(description = "Filter by client account (staff only; client users are always scoped)")
            @RequestParam(required = false) UUID clientAccountId,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return installationJobService.list(actor, clientAccountId, pageable);
    }

    @GetMapping("/{jobId}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get an installation job")
    public InstallationJobResponse get(@AuthenticationPrincipal Actor actor, @PathVariable UUID jobId) {
        return installationJobService.get(actor, jobId);
    }
}
