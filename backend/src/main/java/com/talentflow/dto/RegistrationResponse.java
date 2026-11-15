package com.talentflow.dto;

public record RegistrationResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role
) {
}
