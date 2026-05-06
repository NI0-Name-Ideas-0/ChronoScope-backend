package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Bean Validation validator for the organizationId requirement on static task creation.
 */
public class StaticTaskOrganizationValidator implements ConstraintValidator<ValidStaticTaskOrganization, StaticTaskCreateRequest> {

    @Override
    public boolean isValid(StaticTaskCreateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.isBlocker() == null || Boolean.TRUE.equals(request.isBlocker())
                || request.organizationId() != null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
            .addPropertyNode("organizationId")
            .addConstraintViolation();
        return false;
    }
}
