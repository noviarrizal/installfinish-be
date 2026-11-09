package com.installfinish.job;

import com.installfinish.closeout.CloseoutCaseSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InstallationJobResponse(
        UUID id,
        UUID clientAccountId,
        String externalJobId,
        String jobType,
        IntakeChannel intakeChannel,
        String sourceSystem,
        ServiceLocationResponse serviceLocation,
        String homeownerName,
        String homeownerEmail,
        String homeownerPhone,
        LocalDate soldDate,
        LocalDate installedDate,
        String contractorTeam,
        List<EquipmentItem> equipment,
        CloseoutCaseSummary closeoutCase,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public record ServiceLocationResponse(
            UUID id,
            String addressLine1,
            String addressLine2,
            String city,
            String stateCode,
            String postalCode,
            String county,
            UUID municipalityId) {

        static ServiceLocationResponse from(ServiceLocation location) {
            return new ServiceLocationResponse(location.getId(), location.getAddressLine1(),
                    location.getAddressLine2(), location.getCity(), location.getStateCode(),
                    location.getPostalCode(), location.getCounty(), location.getMunicipalityId());
        }
    }

    static InstallationJobResponse from(InstallationJob job, CloseoutCaseSummary closeoutCase) {
        return new InstallationJobResponse(job.getId(), job.getClientAccountId(), job.getExternalJobId(),
                job.getJobType(), job.getIntakeChannel(), job.getSourceSystem(),
                ServiceLocationResponse.from(job.getServiceLocation()), job.getHomeownerName(),
                job.getHomeownerEmail(), job.getHomeownerPhone(), job.getSoldDate(), job.getInstalledDate(),
                job.getContractorTeam(), job.getEquipment(), closeoutCase, job.getCreatedAt(), job.getUpdatedAt(),
                job.getVersion());
    }
}
