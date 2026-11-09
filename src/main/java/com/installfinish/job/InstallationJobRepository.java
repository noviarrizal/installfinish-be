package com.installfinish.job;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationJobRepository extends JpaRepository<InstallationJob, UUID> {

    @EntityGraph(attributePaths = "serviceLocation")
    Optional<InstallationJob> findWithLocationById(UUID id);

    @EntityGraph(attributePaths = "serviceLocation")
    Optional<InstallationJob> findWithLocationByIdAndClientAccountId(UUID id, UUID clientAccountId);

    @EntityGraph(attributePaths = "serviceLocation")
    Page<InstallationJob> findByClientAccountId(UUID clientAccountId, Pageable pageable);

    @EntityGraph(attributePaths = "serviceLocation")
    Page<InstallationJob> findAllBy(Pageable pageable);

    boolean existsByClientAccountIdAndExternalJobId(UUID clientAccountId, String externalJobId);
}
