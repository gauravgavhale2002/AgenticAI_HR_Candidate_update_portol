package com.talentflow.dto;

import com.talentflow.enums.CandidateHiringStatus;
import java.time.LocalDateTime;

public record CandidateStatusHistoryResponse(
        CandidateHiringStatus status,
        LocalDateTime changedAt
) {
}
