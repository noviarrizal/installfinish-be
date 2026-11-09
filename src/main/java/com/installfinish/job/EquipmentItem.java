package com.installfinish.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An installed equipment unit, stored as part of the job's {@code equipment} JSON array. Serial numbers are
 * sensitive operational data: do not log them or copy them into audit events.
 */
public record EquipmentItem(
        @Schema(example = "CONDENSER") @NotBlank @Size(max = 50) String equipmentType,
        @Size(max = 100) String manufacturer,
        @Size(max = 100) String modelNumber,
        @Size(max = 100) String serialNumber) {
}
