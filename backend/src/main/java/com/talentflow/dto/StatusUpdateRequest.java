package com.talentflow.dto;

import com.talentflow.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Application status is required")
        ApplicationStatus status,

        String reviewer,

        String notes
) {
}
