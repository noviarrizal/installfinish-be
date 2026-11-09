package com.installfinish.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.installfinish.client.CreateClientAccountRequest;
import com.installfinish.client.CreateClientUserRequest;
import com.installfinish.job.CreateInstallationJobRequest;
import com.installfinish.job.CreateInstallationJobRequest.ServiceLocationRequest;
import com.installfinish.job.EquipmentItem;
import com.installfinish.security.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"America/New_York", "America/Phoenix", "Pacific/Honolulu"})
    void acceptsIanaRegionTimeZones(String timezone) {
        assertThat(invalidFields(clientAccount(timezone))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"EST", "UTC+5", "+05:00", "America/Nowhere"})
    void rejectsNonRegionTimeZones(String timezone) {
        assertThat(invalidFields(clientAccount(timezone))).containsExactly("timezone");
    }

    @Test
    void clientUsersCannotBeGivenStaffRoles() {
        var request = new CreateClientUserRequest("idp|123", "a@example.com", "Ann", Role.SYSTEM_ADMIN);

        assertThat(invalidFields(request)).containsExactly("customerRole");
    }

    @Test
    void validJobRequestPasses() {
        assertThat(invalidFields(job("HVAC_REPLACEMENT", location("FL", "33441"), "pat@example.com"))).isEmpty();
    }

    @Test
    void jobRequestReportsEachInvalidField() {
        var request = job("hvac replacement", location("Florida", "3344"), "not-an-email");

        assertThat(invalidFields(request)).containsExactlyInAnyOrder(
                "jobType", "serviceLocation.stateCode", "serviceLocation.postalCode", "homeownerEmail");
    }

    @Test
    void equipmentEntriesAreValidated() {
        var request = new CreateInstallationJobRequest(UUID.randomUUID(), null, "HVAC_REPLACEMENT", null, null,
                location("TX", "75001"), null, null, null, null, null, null,
                List.of(new EquipmentItem(" ", "Carrier", "24ACC636", "SN-1")));

        assertThat(invalidFields(request)).containsExactly("equipment[0].equipmentType");
    }

    private Set<String> invalidFields(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private static CreateClientAccountRequest clientAccount(String timezone) {
        return new CreateClientAccountRequest("Acme HVAC LLC", "Acme HVAC", timezone, null, null);
    }

    private static ServiceLocationRequest location(String state, String zip) {
        return new ServiceLocationRequest("1 Main St", null, "Deerfield Beach", state, zip, "Broward");
    }

    private static CreateInstallationJobRequest job(String jobType, ServiceLocationRequest location, String email) {
        return new CreateInstallationJobRequest(UUID.randomUUID(), "JOB-1", jobType, null, "ServiceTitan", location,
                "Pat Homeowner", email, "555-0100", null, null, "Crew 3", List.of());
    }
}
