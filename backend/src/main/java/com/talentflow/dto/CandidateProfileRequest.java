package com.talentflow.dto;

import com.talentflow.enums.EmploymentType;
import com.talentflow.validation.ValidExperience;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

@ValidExperience
public record CandidateProfileRequest(
        @NotBlank(message = "Candidate first name is required")
        String firstName,

        @NotBlank(message = "Candidate last name is required")
        String lastName,

        @NotBlank(message = "Candidate email is required")
        @Email(message = "Candidate email must be valid")
        String email,

        @NotBlank(message = "Mobile number is required")
        String mobileNumber,

        String alternateMobileNumber,
        LocalDate dateOfBirth,
        String gender,
        String currentCity,
        String state,
        String country,
        String linkedInUrl,
        String portfolioUrl,

        @PositiveOrZero(message = "Total experience years must be zero or greater")
        Integer totalExperienceYears,

        @PositiveOrZero(message = "Total experience months must be zero or greater")
        Integer totalExperienceMonths,

        @PositiveOrZero(message = "Relevant experience years must be zero or greater")
        Integer relevantExperienceYears,

        @PositiveOrZero(message = "Relevant experience months must be zero or greater")
        Integer relevantExperienceMonths,

        String currentCompany,
        String previousCompany,
        String currentDesignation,
        String expectedDesignation,

        BigDecimal currentCtc,
        BigDecimal expectedCtc,

        @PositiveOrZero(message = "Notice period must be zero or greater")
        Integer noticePeriod,

        EmploymentType employmentType,
        String currentEmploymentStatus,
        String preferredWorkLocation,

        @NotNull(message = "Profile completion percentage is required")
        BigDecimal profileCompletionPercentage
) {
}
