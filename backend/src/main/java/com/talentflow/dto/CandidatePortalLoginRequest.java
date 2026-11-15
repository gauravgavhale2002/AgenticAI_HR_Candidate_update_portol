package com.talentflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CandidatePortalLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
