package com.talentflow.service;

import com.talentflow.dto.CandidateProfileRequest;
import com.talentflow.dto.CandidateProfileResponse;
import com.talentflow.entity.Candidate;
import com.talentflow.entity.User;
import com.talentflow.exception.ResourceNotFoundException;
import com.talentflow.repository.CandidateRepository;
import com.talentflow.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;

    public CandidateService(CandidateRepository candidateRepository, UserRepository userRepository) {
        this.candidateRepository = candidateRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CandidateProfileResponse createProfile(Long userId, CandidateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (candidateRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Candidate with email already exists");
        }

        Candidate candidate = new Candidate();
        candidate.setUser(user);
        candidate.setCandidateCode("CAND-" + System.currentTimeMillis());
        candidate.setFirstName(request.firstName());
        candidate.setLastName(request.lastName());
        candidate.setEmail(request.email());
        candidate.setMobileNumber(request.mobileNumber());
        candidate.setAlternateMobileNumber(request.alternateMobileNumber());
        candidate.setDateOfBirth(request.dateOfBirth());
        candidate.setGender(request.gender());
        candidate.setCurrentCity(request.currentCity());
        candidate.setState(request.state());
        candidate.setCountry(request.country());
        candidate.setLinkedInUrl(request.linkedInUrl());
        candidate.setPortfolioUrl(request.portfolioUrl());
        candidate.setTotalExperienceYears(request.totalExperienceYears());
        candidate.setTotalExperienceMonths(request.totalExperienceMonths());
        candidate.setRelevantExperienceYears(request.relevantExperienceYears());
        candidate.setRelevantExperienceMonths(request.relevantExperienceMonths());
        candidate.setCurrentCompany(request.currentCompany());
        candidate.setPreviousCompany(request.previousCompany());
        candidate.setCurrentDesignation(request.currentDesignation());
        candidate.setExpectedDesignation(request.expectedDesignation());
        candidate.setCurrentCtc(request.currentCtc());
        candidate.setExpectedCtc(request.expectedCtc());
        candidate.setNoticePeriod(request.noticePeriod());
        candidate.setEmploymentType(request.employmentType());
        candidate.setCurrentEmploymentStatus(request.currentEmploymentStatus());
        candidate.setPreferredWorkLocation(request.preferredWorkLocation());
        candidate.setProfileCompletionPercentage(request.profileCompletionPercentage() != null ? request.profileCompletionPercentage() : BigDecimal.ZERO);

        Candidate saved = candidateRepository.save(candidate);
        return mapToResponse(saved);
    }

    public List<CandidateProfileResponse> getAllCandidates() {
        return candidateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CandidateProfileResponse getCandidateById(Long id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
        return mapToResponse(candidate);
    }

    private CandidateProfileResponse mapToResponse(Candidate candidate) {
        return new CandidateProfileResponse(
                candidate.getId(),
                candidate.getCandidateCode(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail(),
                candidate.getMobileNumber(),
                candidate.getCurrentCity(),
                candidate.getCountry(),
                candidate.getCurrentDesignation(),
                candidate.getExpectedDesignation(),
                candidate.getCurrentCompany(),
                candidate.getDateOfBirth(),
                candidate.getGender(),
                candidate.getTotalExperienceYears(),
                candidate.getRelevantExperienceYears(),
                candidate.getEmploymentType(),
                candidate.getProfileCompletionPercentage()
        );
    }
}
