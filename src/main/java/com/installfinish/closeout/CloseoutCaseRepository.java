package com.installfinish.closeout;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CloseoutCaseRepository
        extends JpaRepository<CloseoutCase, UUID>, JpaSpecificationExecutor<CloseoutCase> {

    Optional<CloseoutCase> findByIdAndClientAccountId(UUID id, UUID clientAccountId);

    List<CloseoutCase> findByInstallationJobIdIn(Collection<UUID> installationJobIds);
}
