package com.installfinish.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateInstallationJobRequest(
        @NotNull UUID clientAccountId,
        @Schema(description = "The client's own job number (CRM/FSM), unique per client account")
        @Size(max = 100) String externalJobId,
        @Schema(description = "Job type code used to select the client's playbook", example = "HVAC_REPLACEMENT")
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "must be an UPPER_SNAKE_CASE code")
        String jobType,
        @Schema(description = "Defaults to MANUAL")
        IntakeChannel intakeChannel,
        @Schema(description = "Client system the job came from", example = "ServiceTitan")
        @Size(max = 50) String sourceSystem,
        @NotNull @Valid ServiceLocationRequest serviceLocation,
        @Size(max = 200) String homeownerName,
        @Email @Size(max = 320) String homeownerEmail,
        @Size(max = 30) String homeownerPhone,
        LocalDate soldDate,
        LocalDate installedDate,
        @Size(max = 200) String contractorTeam,
        @Size(max = 20) List<@NotNull @Valid EquipmentItem> equipment) {

    public record ServiceLocationRequest(
            @NotBlank @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @NotBlank @Size(max = 100) String city,
            @Schema(example = "FL") @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "must be a two-letter state code")
            String stateCode,
            @Schema(example = "33441") @NotBlank
            @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "must be a US ZIP or ZIP+4 code")
            String postalCode,
            @Size(max = 100) String county) {
    }
}
