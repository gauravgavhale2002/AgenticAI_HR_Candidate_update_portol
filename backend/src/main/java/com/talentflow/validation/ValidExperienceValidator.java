package com.talentflow.validation;

import com.talentflow.dto.CandidateProfileRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidExperienceValidator implements ConstraintValidator<ValidExperience, CandidateProfileRequest> {

    @Override
    public boolean isValid(CandidateProfileRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        Integer totalYears = value.totalExperienceYears();
        Integer totalMonths = value.totalExperienceMonths();
        Integer relevantYears = value.relevantExperienceYears();
        Integer relevantMonths = value.relevantExperienceMonths();

        if (totalYears == null && totalMonths == null && relevantYears == null && relevantMonths == null) {
            return true;
        }

        int totalExperience = (totalYears == null ? 0 : totalYears) * 12 + (totalMonths == null ? 0 : totalMonths);
        int relevantExperience = (relevantYears == null ? 0 : relevantYears) * 12 + (relevantMonths == null ? 0 : relevantMonths);

        if (relevantExperience > totalExperience) {
            return false;
        }

        return true;
    }
}
