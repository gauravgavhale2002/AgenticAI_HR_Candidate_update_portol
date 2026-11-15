package com.talentflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RegistrationRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 80, message = "First name must be at most 80 characters")
        @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{M} .'-]*$", message = "First name must contain letters only")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80, message = "Last name must be at most 80 characters")
        @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{M} .'-]*$", message = "Last name must contain letters only")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(max = 254, message = "Email must be at most 254 characters")
        @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$", message = "Email should be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Enter a valid phone number with country code")
        String phone,

        @NotBlank(message = "Job title is required")
        @Size(max = 120, message = "Job title must be at most 120 characters")
        @Pattern(regexp = "^(?=.*[\\p{L}])[\\p{L}\\p{M}\\p{N} &+#/().,'_-]+$",
                message = "Job title must include at least one letter; numbers are allowed with a title")
        String currentDesignation,

        @NotNull(message = "Years of experience is required")
        Integer experienceYears,

        @NotEmpty(message = "Add at least one skill")
        List<@NotBlank(message = "Skill cannot be blank") String> skills
) {
    @AssertTrue(message = "Each skill must include at least one letter; version numbers are allowed")
    public boolean isSkillListValid() {
        return skills != null && !skills.isEmpty()
                && skills.stream().allMatch(skill -> skill != null
                        && !skill.isBlank()
                        && skill.codePoints().anyMatch(Character::isLetter));
    }
}
