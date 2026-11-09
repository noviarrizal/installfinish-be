package com.installfinish.client;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientAccountRepository extends JpaRepository<ClientAccount, UUID> {
}
