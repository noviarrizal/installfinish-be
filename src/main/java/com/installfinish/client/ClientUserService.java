package com.installfinish.client;

import com.installfinish.audit.AuditAction;
import com.installfinish.audit.AuditService;
import com.installfinish.common.error.ConflictException;
import com.installfinish.security.Actor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientUserService {

    private final ClientUserRepository clientUserRepository;
    private final ClientAccountService clientAccountService;
    private final AuditService auditService;

    public ClientUserService(ClientUserRepository clientUserRepository, ClientAccountService clientAccountService,
                             AuditService auditService) {
        this.clientUserRepository = clientUserRepository;
        this.clientAccountService = clientAccountService;
        this.auditService = auditService;
    }

    @Transactional
    public ClientUserResponse create(Actor actor, UUID clientAccountId, CreateClientUserRequest request) {
        ClientAccount account = clientAccountService.requireActiveAccount(actor, clientAccountId);
        String identitySubject = request.identitySubject().strip();
        String email = request.email().strip();

        if (clientUserRepository.existsByIdentitySubject(identitySubject)) {
            throw new ConflictException("IDENTITY_ALREADY_LINKED",
                    "This identity is already linked to a client user.");
        }
        if (clientUserRepository.existsByClientAccountIdAndEmailIgnoreCase(account.getId(), email)) {
            throw new ConflictException("CLIENT_USER_EMAIL_EXISTS",
                    "A user with this email already exists for the client account.");
        }

        ClientUser user = clientUserRepository.saveAndFlush(
                new ClientUser(account.getId(), identitySubject, email, request.fullName().strip(), request.role()));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("identitySubject", user.getIdentitySubject());
        snapshot.put("role", user.getRole());
        auditService.record(actor, account.getId(), AuditAction.CLIENT_USER_CREATED, user.getId(), snapshot);

        return ClientUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<ClientUserResponse> list(Actor actor, UUID clientAccountId) {
        ClientAccount account = clientAccountService.findAccessible(actor, clientAccountId);
        return clientUserRepository.findByClientAccountIdOrderByFullNameAsc(account.getId()).stream()
                .map(ClientUserResponse::from)
                .toList();
    }
}
