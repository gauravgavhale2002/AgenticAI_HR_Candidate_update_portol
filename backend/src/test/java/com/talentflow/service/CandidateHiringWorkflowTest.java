package com.talentflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.talentflow.dto.CandidateHiringStatusUpdateRequest;
import com.talentflow.dto.RegisteredCandidateResponse;
import com.talentflow.entity.Candidate;
import com.talentflow.entity.CandidateStatusHistory;
import com.talentflow.enums.CandidateHiringStatus;
import com.talentflow.repository.CandidateRepository;
import com.talentflow.repository.CandidateStatusHistoryRepository;
import com.talentflow.repository.JobRepository;
import com.talentflow.repository.ResumeRepository;
import com.talentflow.repository.RoleRepository;
import com.talentflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CandidateHiringWorkflowTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CandidateRepository candidateRepository;
    @Mock private CandidateStatusHistoryRepository historyRepository;
    @Mock private ResumeRepository resumeRepository;
    @Mock private JobRepository jobRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ResumeStorageService resumeStorageService;
    @Mock private ResumeScreeningAgent resumeScreeningAgent;

    private AuthService authService;
    private Candidate candidate;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                candidateRepository,
                historyRepository,
                resumeRepository,
                jobRepository,
                passwordEncoder,
                resumeStorageService,
                resumeScreeningAgent);
        candidate = new Candidate();
        candidate.setId(7L);
        candidate.setFirstName("Test");
        candidate.setLastName("Candidate");
        candidate.setEmail("candidate@example.com");
        candidate.setHiringStatus(CandidateHiringStatus.IN_REVIEW);
        when(candidateRepository.findById(7L)).thenReturn(Optional.of(candidate));
    }

    @Test
    void advancesCandidateToFirstRoundAndRecordsAuditHistory() {
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resumeRepository.findFirstByCandidateIdOrderByUploadedAtDesc(7L)).thenReturn(Optional.empty());
        RegisteredCandidateResponse response = authService.updateHiringStatus(
                7L,
                new CandidateHiringStatusUpdateRequest(
                        CandidateHiringStatus.SELECTED_FOR_FIRST_ROUND, "CV reviewed", null),
                () -> "hr-admin");

        assertEquals(CandidateHiringStatus.SELECTED_FOR_FIRST_ROUND, response.hiringStatus());
        ArgumentCaptor<CandidateStatusHistory> history = ArgumentCaptor.forClass(CandidateStatusHistory.class);
        verify(historyRepository).save(history.capture());
        assertEquals(CandidateHiringStatus.SELECTED_FOR_FIRST_ROUND, history.getValue().getStatus());
        assertEquals("hr-admin", history.getValue().getChangedBy());
        assertEquals("CV reviewed", history.getValue().getRemarks());
    }

    @Test
    void rejectsSkippingHiringStages() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> authService.updateHiringStatus(
                        7L,
                        new CandidateHiringStatusUpdateRequest(CandidateHiringStatus.SELECTED, null, null),
                        () -> "hr-admin"));

        assertEquals(409, error.getStatusCode().value());
        assertEquals(CandidateHiringStatus.IN_REVIEW, candidate.getHiringStatus());
    }

    @Test
    void requiresManagerFeedbackBeforeClearingManagerRound() {
        candidate.setHiringStatus(CandidateHiringStatus.SELECTED_FOR_MANAGER_ROUND);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> authService.updateHiringStatus(
                        7L,
                        new CandidateHiringStatusUpdateRequest(CandidateHiringStatus.SELECTED, null, "  "),
                        () -> "hr-admin"));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getReason().contains("Manager feedback is required"));
        assertEquals(CandidateHiringStatus.SELECTED_FOR_MANAGER_ROUND, candidate.getHiringStatus());
    }

    @Test
    void savesManagerFeedbackWhenClearingManagerRound() {
        candidate.setHiringStatus(CandidateHiringStatus.SELECTED_FOR_MANAGER_ROUND);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resumeRepository.findFirstByCandidateIdOrderByUploadedAtDesc(7L)).thenReturn(Optional.empty());

        RegisteredCandidateResponse response = authService.updateHiringStatus(
                7L,
                new CandidateHiringStatusUpdateRequest(
                        CandidateHiringStatus.SELECTED, "Round completed", "Strong technical skills"),
                () -> "hr-admin");

        assertEquals(CandidateHiringStatus.SELECTED, response.hiringStatus());
        assertEquals("Strong technical skills", response.managerFeedback());
        ArgumentCaptor<CandidateStatusHistory> history = ArgumentCaptor.forClass(CandidateStatusHistory.class);
        verify(historyRepository).save(history.capture());
        assertTrue(history.getValue().getRemarks().contains("Manager feedback: Strong technical skills"));
    }
}
