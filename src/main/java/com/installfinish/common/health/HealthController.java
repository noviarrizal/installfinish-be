package com.installfinish.common.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health")
public class HealthController {

    private final HealthEndpoint healthEndpoint;
    private final String version;

    public HealthController(HealthEndpoint healthEndpoint, ObjectProvider<BuildProperties> buildProperties) {
        this.healthEndpoint = healthEndpoint;
        BuildProperties build = buildProperties.getIfAvailable();
        this.version = build != null ? build.getVersion() : "unknown";
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Service health",
            description = "Aggregated health including database connectivity. Returns 503 when the service is not UP. "
                    + "Component details are intentionally not exposed.")
    public ResponseEntity<HealthResponse> health() {
        Status status = healthEndpoint.health().getStatus();
        HttpStatus httpStatus = Status.UP.equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus)
                .body(new HealthResponse(status.getCode(), "installfinish-be", version, Instant.now()));
    }

    public record HealthResponse(String status, String service, String version, Instant checkedAt) {
    }
}
