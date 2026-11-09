package com.installfinish.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientUserRepository extends JpaRepository<ClientUser, UUID> {

    Optional<ClientUser> findByIdentitySubject(String identitySubject);

    boolean existsByIdentitySubject(String identitySubject);

    boolean existsByClientAccountIdAndEmailIgnoreCase(UUID clientAccountId, String email);

    List<ClientUser> findByClientAccountIdOrderByFullNameAsc(UUID clientAccountId);
}
