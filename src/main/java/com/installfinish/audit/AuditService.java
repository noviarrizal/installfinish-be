package com.installfinish.audit;

import com.installfinish.audit.AuditEvent.ActorType;
import com.installfinish.common.web.RequestIdFilter;
import com.installfinish.security.Actor;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final String SOURCE_API = "api";

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Records an action taken by an authenticated actor. Requires an existing transaction so the audit event commits
     * or rolls back together with the change it describes.
     *
     * @param changes creation snapshot or before/after values; must not contain secrets or document contents
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Actor actor, UUID clientAccountId, AuditAction action, UUID entityId,
                       Map<String, Object> changes) {
        ActorType actorType = actor.isStaff() ? ActorType.STAFF : ActorType.CLIENT_USER;
        auditEventRepository.save(new AuditEvent(clientAccountId, actorType, actor.subject(), action, entityId,
                changes, MDC.get(RequestIdFilter.MDC_KEY), MDC.get(RequestIdFilter.MDC_CLIENT_IP), SOURCE_API));
    }
}
