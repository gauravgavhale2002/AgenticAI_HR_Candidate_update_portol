package com.talentflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "resumes")
public class Resume extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String storagePath;

    private int versionNumber;
    private boolean primaryResume;
    private LocalDateTime uploadedAt;

    @Column(length = 1500)
    private String screeningSummary;

    @Column(length = 1500)
    private String reviewFlags;

    @Column(length = 2000)
    private String extractedSkills;

    private Integer extractedExperienceYears;
    private Integer screeningScore;
}
