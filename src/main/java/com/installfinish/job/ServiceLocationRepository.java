package com.installfinish.job;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceLocationRepository extends JpaRepository<ServiceLocation, UUID> {
}
