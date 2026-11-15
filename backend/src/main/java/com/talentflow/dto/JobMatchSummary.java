package com.talentflow.dto;

import java.util.List;

public record JobMatchSummary(
        Long jobId,
        String title,
        int matchScore,
        List<String> matchedSkills,
        List<String> missingSkills
) {
}
