package com.talentflow.controller;

import com.talentflow.dto.CandidateProfileRequest;
import com.talentflow.dto.CandidateProfileResponse;
import com.talentflow.service.CandidateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<CandidateProfileResponse> createCandidateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody CandidateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidateService.createProfile(userId, request));
    }

    @GetMapping
    public List<CandidateProfileResponse> getAllCandidates() {
        return candidateService.getAllCandidates();
    }

    @GetMapping("/{id}")
    public CandidateProfileResponse getCandidateById(@PathVariable Long id) {
        return candidateService.getCandidateById(id);
    }
}
