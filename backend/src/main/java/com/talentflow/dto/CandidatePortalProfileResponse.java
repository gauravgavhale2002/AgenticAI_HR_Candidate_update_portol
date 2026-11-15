package com.talentflow.dto;

import com.talentflow.enums.CandidateHiringStatus;
import java.util.List;

public record CandidatePortalProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String currentDesignation,
        Integer experienceYears,
        List<String> skills,
        CandidateHiringStatus hiringStatus
) {
}
