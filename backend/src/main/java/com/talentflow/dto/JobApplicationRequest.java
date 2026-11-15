package com.talentflow.dto;

import com.talentflow.enums.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record JobApplicationRequest(
        @NotNull(message = "Candidate ID is required")
        Long candidateId,

        @NotNull(message = "Job ID is required")
        Long jobId,

        @NotBlank(message = "Applicant name is required")
        String applicantName,

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Cover letter is required")
        String coverLetter,

        EmploymentType employmentType,

        @Positive(message = "Years of experience must be positive")
        Integer yearsOfExperience,

        List<String> skills,

        String portfolioUrl,

        String linkedInUrl,

        String expectedSalary
) {
}
