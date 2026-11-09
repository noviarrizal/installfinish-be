package com.installfinish.closeout;

import com.installfinish.common.api.PageResponse;
import com.installfinish.security.Actor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "Closeout cases")
@PreAuthorize("hasAnyRole('OPERATOR', 'QUALITY_REVIEWER', 'CUSTOMER_APPROVER', 'CUSTOMER_VIEWER')")
public class CloseoutCaseController {

    private final CloseoutCaseService closeoutCaseService;

    public CloseoutCaseController(CloseoutCaseService closeoutCaseService) {
        this.closeoutCaseService = closeoutCaseService;
    }

    @GetMapping
    @Operation(summary = "Search the closeout queue",
            description = "Client users only ever see their own account's cases.")
    public PageResponse<CloseoutCaseResponse> search(
            @AuthenticationPrincipal Actor actor,
            @Parameter(description = "Filter by client account (staff only; client users are always scoped)")
            @RequestParam(required = false) UUID clientAccountId,
            @RequestParam(required = false) CloseoutCaseStatus status,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return closeoutCaseService.search(actor, clientAccountId, status, pageable);
    }

    @GetMapping("/{caseId}")
    @Operation(summary = "Get a closeout case")
    public CloseoutCaseResponse get(@AuthenticationPrincipal Actor actor, @PathVariable UUID caseId) {
        return closeoutCaseService.get(actor, caseId);
    }
}
