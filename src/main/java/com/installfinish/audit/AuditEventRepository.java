package com.installfinish.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Insert and read only: deliberately not a CrudRepository, so no delete methods exist. */
public interface AuditEventRepository extends Repository<AuditEvent, UUID> {

    AuditEvent save(AuditEvent event);

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, UUID entityId);
}
