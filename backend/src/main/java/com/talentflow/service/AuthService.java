package com.talentflow.service;

import com.talentflow.dto.RegistrationRequest;
import com.talentflow.dto.RegistrationResponse;
import com.talentflow.dto.RegisteredCandidateResponse;
import com.talentflow.dto.JobMatchSummary;
import com.talentflow.dto.CandidateHiringStatusUpdateRequest;
import com.talentflow.dto.CandidatePortalLoginRequest;
import com.talentflow.dto.CandidatePortalProfileResponse;
import com.talentflow.dto.CandidatePortalResponse;
import com.talentflow.dto.CandidateStatusHistoryResponse;
import com.talentflow.entity.Candidate;
import com.talentflow.entity.CandidateSkill;
import com.talentflow.entity.CandidateStatusHistory;
import com.talentflow.entity.Resume;
import com.talentflow.entity.Role;
import com.talentflow.entity.User;
import com.talentflow.enums.CandidateHiringStatus;
import com.talentflow.enums.RoleName;
import com.talentflow.repository.CandidateRepository;
import com.talentflow.repository.CandidateStatusHistoryRepository;
import com.talentflow.repository.JobRepository;
import com.talentflow.repository.ResumeRepository;
import com.talentflow.repository.RoleRepository;
import com.talentflow.repository.UserRepository;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateStatusHistoryRepository candidateStatusHistoryRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResumeStorageService resumeStorageService;
    private final ResumeScreeningAgent resumeScreeningAgent;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CandidateRepository candidateRepository,
            CandidateStatusHistoryRepository candidateStatusHistoryRepository,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            PasswordEncoder passwordEncoder,
            ResumeStorageService resumeStorageService,
            ResumeScreeningAgent resumeScreeningAgent) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.candidateRepository = candidateRepository;
        this.candidateStatusHistoryRepository = candidateStatusHistoryRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.passwordEncoder = passwordEncoder;
        this.resumeStorageService = resumeStorageService;
        this.resumeScreeningAgent = resumeScreeningAgent;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request, MultipartFile resumeFile) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        try {
            if (!PhoneNumberUtil.getInstance().isValidNumber(
                    PhoneNumberUtil.getInstance().parse(request.phone(), null))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Enter a valid phone number for the selected country");
            }
        } catch (NumberParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a valid phone number with country code, for example +14155552671", exception);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String resumeContentType = resumeStorageService.validateAndGetContentType(resumeFile);
        ResumeScreeningAgent.ScreeningResult screening = resumeScreeningAgent.analyze(resumeFile);

        Role candidateRole = roleRepository.findByName(RoleName.CANDIDATE)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.CANDIDATE);
                    role.setDescription("Candidate account");
                    return roleRepository.save(role);
                });

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(candidateRole));

        User saved = userRepository.save(user);
        Candidate candidate = new Candidate();
        candidate.setUser(saved);
        candidate.setCandidateCode("CAND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        candidate.setFirstName(saved.getFirstName());
        candidate.setLastName(saved.getLastName());
        candidate.setEmail(saved.getEmail());
        candidate.setMobileNumber(saved.getPhone());
        candidate.setCurrentDesignation(request.currentDesignation());
        candidate.setTotalExperienceYears(request.experienceYears());
        candidate.setProfileCompletionPercentage(java.math.BigDecimal.valueOf(40));
        candidate.setHiringStatus(CandidateHiringStatus.IN_REVIEW);
        candidate.setHiringStatusUpdatedAt(LocalDateTime.now());
        for (String skillName : request.skills()) {
            CandidateSkill skill = new CandidateSkill();
            skill.setCandidate(candidate);
            skill.setSkillName(skillName.trim());
            candidate.getSkills().add(skill);
        }
        Candidate savedCandidate = candidateRepository.save(candidate);
        saveStatusHistory(savedCandidate, CandidateHiringStatus.IN_REVIEW, "SYSTEM",
                "Profile submitted; waiting for HR review.");

        try {
            Resume resume = new Resume();
            resume.setCandidate(savedCandidate);
            resume.setOriginalFileName(resumeFile.getOriginalFilename());
            resume.setContentType(resumeContentType);
            resume.setFileSize(resumeFile.getSize());
            resume.setStoragePath(resumeStorageService.store(resumeFile));
            resume.setFileName(resumeFile.getOriginalFilename());
            resume.setVersionNumber(1);
            resume.setPrimaryResume(true);
            resume.setUploadedAt(LocalDateTime.now());
            resume.setScreeningSummary(screening.summary());
            resume.setExtractedSkills(String.join(",", screening.extractedSkills()));
            resume.setExtractedExperienceYears(screening.experienceYears());
            resume.setScreeningScore(screening.confidence());
            resumeRepository.save(resume);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save resume", exception);
        }

        return new RegistrationResponse(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getPhone(),
                RoleName.CANDIDATE.name()
        );
    }

    @Transactional(readOnly = true)
    public List<RegisteredCandidateResponse> getRegisteredCandidates() {
        return candidateRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(this::toRegisteredCandidateResponse)
                .toList();
    }

    @Transactional
    public void deleteCandidate(Long candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));
        User user = candidate.getUser();
        List<String> resumePaths = candidate.getResumes().stream()
                .map(Resume::getStoragePath)
                .toList();

        candidateRepository.delete(candidate);
        if (user != null) {
            userRepository.delete(user);
        }

        Runnable deleteResumeFiles = () -> resumePaths.forEach(path -> {
            try {
                resumeStorageService.delete(path);
            } catch (IOException exception) {
                logger.error("Could not delete resume file for deleted candidate {}", candidateId, exception);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteResumeFiles.run();
                }
            });
        } else {
            deleteResumeFiles.run();
        }
    }

    @Transactional(readOnly = true)
    public CandidatePortalResponse loginCandidate(CandidatePortalLoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Candidate candidate = candidateRepository.findByEmail(email)
                .filter(found -> found.getUser() != null
                        && passwordEncoder.matches(request.password(), found.getUser().getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Email or password is incorrect"));

        List<CandidateStatusHistoryResponse> history = candidateStatusHistoryRepository
                .findByCandidateIdOrderByChangedAtAsc(candidate.getId()).stream()
                .map(entry -> new CandidateStatusHistoryResponse(
                        entry.getStatus(),
                        entry.getChangedAt()))
                .toList();
        CandidatePortalProfileResponse profile = new CandidatePortalProfileResponse(
                candidate.getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail(),
                candidate.getCurrentDesignation(),
                candidate.getTotalExperienceYears(),
                candidate.getSkills().stream().map(CandidateSkill::getSkillName).toList(),
                candidate.getHiringStatus());
        return new CandidatePortalResponse(profile, history);
    }

    @Transactional
    public RegisteredCandidateResponse updateHiringStatus(
            Long candidateId,
            CandidateHiringStatusUpdateRequest request,
            Principal principal) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));
        CandidateHiringStatus current = candidate.getHiringStatus();
        CandidateHiringStatus next = request.status();

        if (current == CandidateHiringStatus.OFFER_RELEASED || current == CandidateHiringStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This hiring process is already complete");
        }
        if (next != CandidateHiringStatus.REJECTED && !isNextHiringStatus(current, next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hiring stages must be completed in order");
        }
        if (current == CandidateHiringStatus.SELECTED_FOR_MANAGER_ROUND
                && next == CandidateHiringStatus.SELECTED
                && (request.managerFeedback() == null || request.managerFeedback().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Manager feedback is required before marking the candidate selected");
        }

        candidate.setHiringStatus(next);
        candidate.setHiringStatusUpdatedAt(LocalDateTime.now());
        if (request.managerFeedback() != null && !request.managerFeedback().isBlank()) {
            candidate.setManagerFeedback(request.managerFeedback().trim());
        }
        candidateRepository.save(candidate);

        String notes = request.notes() == null ? "" : request.notes().trim();
        String managerFeedback = request.managerFeedback() == null ? "" : request.managerFeedback().trim();
        String remarks = managerFeedback.isBlank()
                ? notes
                : notes.isBlank() ? "Manager feedback: " + managerFeedback
                        : notes + "\nManager feedback: " + managerFeedback;
        saveStatusHistory(candidate, next, principal.getName(), remarks);
        return toRegisteredCandidateResponse(candidate);
    }

    private RegisteredCandidateResponse toRegisteredCandidateResponse(Candidate candidate) {
        Resume resume = resumeRepository.findFirstByCandidateIdOrderByUploadedAtDesc(candidate.getId())
                .orElse(null);
        return new RegisteredCandidateResponse(
                candidate.getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail(),
                candidate.getMobileNumber(),
                candidate.getCreatedAt(),
                candidate.getCurrentDesignation(),
                candidate.getTotalExperienceYears(),
                candidate.getSkills().stream().map(CandidateSkill::getSkillName).toList(),
                resume == null ? null : resume.getId(),
                resume == null ? null : resume.getOriginalFileName(),
                resume == null || resume.getExtractedSkills() == null || resume.getExtractedSkills().isBlank()
                        ? List.of()
                        : List.of(resume.getExtractedSkills().split(",")),
                resume == null ? null : resume.getExtractedExperienceYears(),
                resume == null ? null : resume.getScreeningScore(),
                resume == null ? null : resume.getScreeningSummary(),
                resume == null ? List.of() : matchJobs(resume.getExtractedSkills()),
                candidate.getHiringStatus(),
                candidate.getHiringStatusUpdatedAt(),
                candidate.getManagerFeedback());
    }

    private boolean isNextHiringStatus(CandidateHiringStatus current, CandidateHiringStatus next) {
        return switch (current) {
            case IN_REVIEW -> next == CandidateHiringStatus.SELECTED_FOR_FIRST_ROUND;
            case SELECTED_FOR_FIRST_ROUND -> next == CandidateHiringStatus.SELECTED_FOR_MANAGER_ROUND;
            case SELECTED_FOR_MANAGER_ROUND -> next == CandidateHiringStatus.SELECTED;
            case SELECTED -> next == CandidateHiringStatus.OFFER_RELEASED;
            case OFFER_RELEASED, REJECTED -> false;
        };
    }

    private void saveStatusHistory(
            Candidate candidate,
            CandidateHiringStatus status,
            String changedBy,
            String remarks) {
        CandidateStatusHistory history = new CandidateStatusHistory();
        history.setCandidate(candidate);
        history.setStatus(status);
        history.setChangedBy(changedBy);
        history.setChangedAt(LocalDateTime.now());
        history.setRemarks(remarks);
        candidateStatusHistoryRepository.save(history);
    }

    private List<JobMatchSummary> matchJobs(String extractedSkillsText) {
        Set<String> candidateSkills = extractedSkillsText == null
                ? Set.of()
                : java.util.Arrays.stream(extractedSkillsText.split(","))
                        .map(skill -> skill.trim().toLowerCase(Locale.ROOT))
                        .filter(skill -> !skill.isBlank())
                        .collect(java.util.stream.Collectors.toSet());
        if (candidateSkills.isEmpty()) {
            return List.of();
        }

        return jobRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(job -> {
                    List<String> requiredSkills = job.getSkillRequirements().stream()
                            .map(requirement -> requirement.getSkillName().trim())
                            .distinct()
                            .toList();
                    if (requiredSkills.isEmpty()) {
                        return null;
                    }
                    List<String> matched = requiredSkills.stream()
                            .filter(skill -> candidateSkills.contains(skill.toLowerCase(Locale.ROOT)))
                            .toList();
                    List<String> missing = requiredSkills.stream()
                            .filter(skill -> !candidateSkills.contains(skill.toLowerCase(Locale.ROOT)))
                            .toList();
                    int score = Math.round((float) matched.size() * 100 / requiredSkills.size());
                    return new JobMatchSummary(job.getId(), job.getTitle(), score, matched, missing);
                })
                .filter(java.util.Objects::nonNull)
                .sorted((left, right) -> Integer.compare(right.matchScore(), left.matchScore()))
                .limit(3)
                .toList();
    }

    @Transactional(readOnly = true)
    public Resume getResume(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
    }
}
