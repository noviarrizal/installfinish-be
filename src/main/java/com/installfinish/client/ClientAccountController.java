package com.installfinish.client;

import com.installfinish.common.api.PageResponse;
import com.installfinish.security.Actor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client-accounts")
@Tag(name = "Client accounts")
public class ClientAccountController {

    private final ClientAccountService clientAccountService;
    private final ClientUserService clientUserService;

    public ClientAccountController(ClientAccountService clientAccountService, ClientUserService clientUserService) {
        this.clientAccountService = clientAccountService;
        this.clientUserService = clientUserService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Onboard a client account")
    public ResponseEntity<ClientAccountResponse> create(@AuthenticationPrincipal Actor actor,
                                                        @Valid @RequestBody CreateClientAccountRequest request) {
        ClientAccountResponse created = clientAccountService.create(actor, request);
        return ResponseEntity.created(URI.create("/api/v1/client-accounts/" + created.id())).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATOR', 'QUALITY_REVIEWER')")
    @Operation(summary = "List client accounts")
    public PageResponse<ClientAccountResponse> list(
            @AuthenticationPrincipal Actor actor,
            @ParameterObject @PageableDefault(sort = "displayName", direction = Sort.Direction.ASC) Pageable pageable) {
        return clientAccountService.list(actor, pageable);
    }

    @GetMapping("/{clientAccountId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a client account", description = "Client users can only read their own account.")
    public ClientAccountResponse get(@AuthenticationPrincipal Actor actor, @PathVariable UUID clientAccountId) {
        return clientAccountService.get(actor, clientAccountId);
    }

    @PostMapping("/{clientAccountId}/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Link a client-side user (approver or viewer) to a client account")
    public ResponseEntity<ClientUserResponse> createUser(@AuthenticationPrincipal Actor actor,
                                                         @PathVariable UUID clientAccountId,
                                                         @Valid @RequestBody CreateClientUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientUserService.create(actor, clientAccountId, request));
    }

    @GetMapping("/{clientAccountId}/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List a client account's users")
    public List<ClientUserResponse> listUsers(@AuthenticationPrincipal Actor actor,
                                              @PathVariable UUID clientAccountId) {
        return clientUserService.list(actor, clientAccountId);
    }
}
