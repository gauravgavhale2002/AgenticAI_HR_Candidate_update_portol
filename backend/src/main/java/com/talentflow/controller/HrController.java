package com.talentflow.controller;

import com.talentflow.dto.JobApplicationRequest;
import com.talentflow.dto.RegisteredCandidateResponse;
import com.talentflow.dto.StatusUpdateRequest;
import com.talentflow.dto.CandidateHiringStatusUpdateRequest;
import com.talentflow.entity.JobApplication;
import com.talentflow.entity.Resume;
import com.talentflow.service.AuthService;
import com.talentflow.service.JobApplicationService;
import com.talentflow.service.ResumeStorageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final JobApplicationService jobApplicationService;
    private final AuthService authService;
    private final ResumeStorageService resumeStorageService;

    public HrController(
            JobApplicationService jobApplicationService,
            AuthService authService,
            ResumeStorageService resumeStorageService) {
        this.jobApplicationService = jobApplicationService;
        this.authService = authService;
        this.resumeStorageService = resumeStorageService;
    }

    @GetMapping("/candidates")
    public List<RegisteredCandidateResponse> getRegisteredCandidates() {
        return authService.getRegisteredCandidates();
    }

    @PostMapping("/candidates/{candidateId}/status")
    public RegisteredCandidateResponse updateCandidateStatus(
            @PathVariable Long candidateId,
            @Valid @RequestBody CandidateHiringStatusUpdateRequest request,
            Principal principal) {
        return authService.updateHiringStatus(candidateId, request, principal);
    }

    @GetMapping("/candidates/resumes/{resumeId}")
    public ResponseEntity<Resource> getCandidateResume(@PathVariable Long resumeId) throws IOException {
        Resume resume = authService.getResume(resumeId);
        Resource file = new FileSystemResource(resumeStorageService.resolve(resume.getStoragePath()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resume.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(resume.getOriginalFileName().replaceAll("[\\r\\n\"]", ""))
                        .build().toString())
                .body(file);
    }

    @PostMapping("/applications")
    public ResponseEntity<JobApplication> submitApplication(@Valid @RequestBody JobApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobApplicationService.submitApplication(request));
    }

    @GetMapping("/applications/candidate/{candidateId}")
    public List<JobApplication> getApplicationsForCandidate(@PathVariable Long candidateId) {
        return jobApplicationService.getApplicationsForCandidate(candidateId);
    }

    @GetMapping("/applications/job/{jobId}")
    public List<JobApplication> getApplicationsForJob(@PathVariable Long jobId) {
        return jobApplicationService.getApplicationsForJob(jobId);
    }

    @PostMapping("/applications/{applicationId}/status")
    public ResponseEntity<JobApplication> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok().build();
    }
}
