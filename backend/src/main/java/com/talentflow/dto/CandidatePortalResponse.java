package com.talentflow.dto;

import java.util.List;

public record CandidatePortalResponse(
        CandidatePortalProfileResponse profile,
        List<CandidateStatusHistoryResponse> statusHistory
) {
}
