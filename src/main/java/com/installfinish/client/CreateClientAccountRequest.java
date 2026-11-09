package com.installfinish.client;

import com.installfinish.common.validation.IanaTimeZone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientAccountRequest(
        @NotBlank @Size(max = 200) String legalName,
        @NotBlank @Size(max = 200) String displayName,
        @Schema(example = "America/New_York") @NotBlank @IanaTimeZone String timezone,
        @Schema(description = "Free-text description of the counties/municipalities served")
        @Size(max = 500) String operatingArea,
        @Size(max = 50) String contractTier) {
}
