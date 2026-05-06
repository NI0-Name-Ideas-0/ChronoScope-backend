package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.Email;

public record InviteUserRequest(
        @Email String targetMail
) {
}
