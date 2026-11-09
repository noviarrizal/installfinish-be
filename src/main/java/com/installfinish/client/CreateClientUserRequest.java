package com.installfinish.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.installfinish.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClientUserRequest(
        @Schema(description = "The 'sub' claim the identity provider issues for this person")
        @NotBlank @Size(max = 255) String identitySubject,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 200) String fullName,
        @Schema(allowableValues = {"CUSTOMER_APPROVER", "CUSTOMER_VIEWER"})
        @NotNull Role role) {

    @JsonIgnore
    @AssertTrue(message = "role must be CUSTOMER_APPROVER or CUSTOMER_VIEWER")
    public boolean isCustomerRole() {
        return role == null || !role.isStaff();
    }
}
