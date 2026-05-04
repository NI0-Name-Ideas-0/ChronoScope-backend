package de.ni0.chronoscope.controller.dto.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Class-level constraint requiring {@code organizationId} unless the static task is a blocker.
 */
@Documented
@Constraint(validatedBy = StaticTaskOrganizationValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStaticTaskOrganization {
    String message() default "organizationId is required unless isBlocker is true";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
