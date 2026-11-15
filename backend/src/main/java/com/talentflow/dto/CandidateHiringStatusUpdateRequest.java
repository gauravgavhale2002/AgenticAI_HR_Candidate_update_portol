package com.talentflow.dto;

import com.talentflow.enums.CandidateHiringStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CandidateHiringStatusUpdateRequest(
        @NotNull CandidateHiringStatus status,
        @Size(max = 1500) String notes,
        @Size(max = 1500) String managerFeedback
) {
}
