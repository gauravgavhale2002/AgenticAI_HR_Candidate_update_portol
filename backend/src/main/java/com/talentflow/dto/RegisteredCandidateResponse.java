package com.talentflow.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.talentflow.enums.CandidateHiringStatus;

public record RegisteredCandidateResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDateTime registeredAt,
        String currentDesignation,
        Integer experienceYears,
        List<String> skills,
        Long resumeId,
        String resumeFileName,
        List<String> cvSkills,
        Integer cvExperienceYears,
        Integer resumeScreeningScore,
        String resumeScreeningSummary,
        List<String> reviewFlags,
        List<JobMatchSummary> jobMatches,
        CandidateHiringStatus hiringStatus,
        LocalDateTime hiringStatusUpdatedAt,
        String managerFeedback
) {
}
