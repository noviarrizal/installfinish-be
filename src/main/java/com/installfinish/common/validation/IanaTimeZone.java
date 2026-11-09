package com.installfinish.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.ZoneId;
import java.util.Set;

/** The value must be an IANA region time zone such as {@code America/New_York}; {@code null} is allowed. */
@Documented
@Constraint(validatedBy = IanaTimeZone.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface IanaTimeZone {

    String message() default "must be an IANA time zone such as America/New_York";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<IanaTimeZone, String> {

        private static final Set<String> REGION_IDS = Set.copyOf(ZoneId.getAvailableZoneIds());

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // Region ids only: offsets like "+05:00" or "UTC+5" would drop DST rules for US jurisdictions.
            return value == null || (value.contains("/") && REGION_IDS.contains(value));
        }
    }
}
