package com.talentflow.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNumbersInCandidateNames() {
        RegistrationRequest request = request("R1ya", "Sharma", "riya@example.com", "+919876543210");

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("firstName")));
    }

    @Test
    void rejectsMalformedEmailAddresses() {
        RegistrationRequest request = request("Riya", "Sharma", "riya@invalid", "+919876543210");

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));
    }

    @Test
    void requiresPhoneNumbersInInternationalFormat() {
        RegistrationRequest request = request("Riya", "Sharma", "riya@example.com", "9876543210");

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phone")));
    }

    @Test
    void acceptsSkillsWithVersionNumbers() {
        RegistrationRequest request = request("Riya", "Sharma", "riya@example.com", "+919876543210",
                List.of("Java8", "SpringBoot3.8"));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsSkillsContainingOnlyNumbers() {
        RegistrationRequest request = request("Riya", "Sharma", "riya@example.com", "+919876543210",
                List.of("1111"));

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("skillListValid")));
    }

    @Test
    void acceptsEmailAddressesWithNumericLocalParts() {
        RegistrationRequest request = request("Riya", "Sharma", "1111@gmail.com", "+919876543210");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsJobTitlesContainingOnlyNumbers() {
        RegistrationRequest request = requestWithJobTitle("1111");

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("currentDesignation")));
    }

    @Test
    void acceptsJobTitlesWithSeniorityOrVersionNumbers() {
        RegistrationRequest request = requestWithJobTitle("Spring Boot Developer 3.8");

        assertTrue(validator.validate(request).isEmpty());
    }

    private RegistrationRequest request(String firstName, String lastName, String email, String phone) {
        return request(firstName, lastName, email, phone, List.of("Java"));
    }

    private RegistrationRequest request(String firstName, String lastName, String email, String phone,
            List<String> skills) {
        return new RegistrationRequest(
                firstName, lastName, email, "securePassword123", phone,
                "Engineer", 3, skills);
    }

    private RegistrationRequest requestWithJobTitle(String jobTitle) {
        return new RegistrationRequest(
                "Riya", "Sharma", "riya@example.com", "securePassword123", "+919876543210",
                jobTitle, 3, List.of("Java"));
    }
}
