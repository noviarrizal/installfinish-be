package com.installfinish.client;

import com.installfinish.audit.AuditAction;
import com.installfinish.audit.AuditService;
import com.installfinish.common.api.PageResponse;
import com.installfinish.common.error.ConflictException;
import com.installfinish.common.error.ResourceNotFoundException;
import com.installfinish.security.Actor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientAccountService {

    private final ClientAccountRepository clientAccountRepository;
    private final AuditService auditService;

    public ClientAccountService(ClientAccountRepository clientAccountRepository, AuditService auditService) {
        this.clientAccountRepository = clientAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ClientAccountResponse create(Actor actor, CreateClientAccountRequest request) {
        ClientAccount account = clientAccountRepository.saveAndFlush(new ClientAccount(
                request.legalName().strip(),
                request.displayName().strip(),
                request.timezone(),
                request.operatingArea(),
                request.contractTier()));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("legalName", account.getLegalName());
        snapshot.put("displayName", account.getDisplayName());
        snapshot.put("timezone", account.getTimezone());
        snapshot.put("contractTier", account.getContractTier());
        snapshot.put("status", account.getStatus());
        auditService.record(actor, account.getId(), AuditAction.CLIENT_ACCOUNT_CREATED, account.getId(), snapshot);

        return ClientAccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientAccountResponse> list(Actor actor, Pageable pageable) {
        if (!actor.isStaff()) {
            throw new AccessDeniedException("Only InstallFinish staff can list client accounts");
        }
        return PageResponse.from(clientAccountRepository.findAll(pageable), ClientAccountResponse::from);
    }

    @Transactional(readOnly = true)
    public ClientAccountResponse get(Actor actor, UUID clientAccountId) {
        return ClientAccountResponse.from(findAccessible(actor, clientAccountId));
    }

    /**
     * Loads a client account the actor may act on and that is currently active. Used by features that create
     * client-owned records.
     */
    @Transactional(readOnly = true)
    public ClientAccount requireActiveAccount(Actor actor, UUID clientAccountId) {
        ClientAccount account = findAccessible(actor, clientAccountId);
        if (!account.isActive()) {
            throw new ConflictException("CLIENT_ACCOUNT_NOT_ACTIVE",
                    "Client account " + clientAccountId + " is " + account.getStatus() + " and cannot receive new work.");
        }
        return account;
    }

    /** Out-of-scope accounts are reported as not found so their existence is not disclosed. */
    ClientAccount findAccessible(Actor actor, UUID clientAccountId) {
        if (!actor.canAccessClient(clientAccountId)) {
            throw new ResourceNotFoundException("Client account", clientAccountId);
        }
        return clientAccountRepository.findById(clientAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Client account", clientAccountId));
    }
}
