package com.talentflow.dto;

import com.talentflow.enums.EmploymentType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CandidateProfileResponse(
        Long id,
        String candidateCode,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        String currentCity,
        String country,
        String currentDesignation,
        String expectedDesignation,
        String currentCompany,
        LocalDate dateOfBirth,
        String gender,
        Integer totalExperienceYears,
        Integer relevantExperienceYears,
        EmploymentType employmentType,
        BigDecimal profileCompletionPercentage
) {
}
