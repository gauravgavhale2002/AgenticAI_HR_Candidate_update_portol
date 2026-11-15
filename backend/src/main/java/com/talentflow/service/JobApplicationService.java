package com.talentflow.service;

import com.talentflow.dto.JobApplicationRequest;
import com.talentflow.entity.Candidate;
import com.talentflow.entity.Job;
import com.talentflow.entity.JobApplication;
import com.talentflow.enums.ApplicationStatus;
import com.talentflow.exception.ResourceNotFoundException;
import com.talentflow.repository.CandidateRepository;
import com.talentflow.repository.JobApplicationRepository;
import com.talentflow.repository.JobRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;

    public JobApplicationService(JobApplicationRepository jobApplicationRepository,
                                 CandidateRepository candidateRepository,
                                 JobRepository jobRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.candidateRepository = candidateRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public JobApplication submitApplication(JobApplicationRequest request) {
        Candidate candidate = candidateRepository.findById(request.candidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + request.candidateId()));
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + request.jobId()));

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedAt(LocalDateTime.now());
        application.setLastStatusUpdatedAt(LocalDateTime.now());
        application.setAiSummary("Application submitted for review");

        return jobApplicationRepository.save(application);
    }

    public List<JobApplication> getApplicationsForCandidate(Long candidateId) {
        return jobApplicationRepository.findByCandidateId(candidateId);
    }

    public List<JobApplication> getApplicationsForJob(Long jobId) {
        return jobApplicationRepository.findByJobId(jobId);
    }
}
