package com.talentflow.controller;

import com.talentflow.dto.RegistrationRequest;
import com.talentflow.dto.RegistrationResponse;
import com.talentflow.dto.CandidatePortalLoginRequest;
import com.talentflow.dto.CandidatePortalResponse;
import com.talentflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestPart("registration") RegistrationRequest request,
            @RequestPart("resume") MultipartFile resume) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, resume));
    }

    @PostMapping("/candidate-login")
    public CandidatePortalResponse candidateLogin(@Valid @RequestBody CandidatePortalLoginRequest request) {
        return authService.loginCandidate(request);
    }
}
