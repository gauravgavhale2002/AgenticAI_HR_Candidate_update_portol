package com.talentflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.talentflow.enums.CandidateHiringStatus;
import com.talentflow.enums.EmploymentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "candidates")
public class Candidate extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String candidateCode;

    @Column(nullable = false)
    private String firstName;

    @Column(length = 80)
    private String middleName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String mobileNumber;

    private String alternateMobileNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String currentCity;
    private String state;
    private String country;
    private String linkedInUrl;
    private String portfolioUrl;

    private Integer totalExperienceYears;
    private Integer totalExperienceMonths;
    private Integer relevantExperienceYears;
    private Integer relevantExperienceMonths;
    private String currentCompany;
    private String previousCompany;
    private String currentDesignation;
    private String expectedDesignation;

    private BigDecimal currentCtc;
    private BigDecimal expectedCtc;
    private Integer noticePeriod;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    private String currentEmploymentStatus;
    private String preferredWorkLocation;

    @Column(precision = 5, scale = 2)
    private BigDecimal profileCompletionPercentage = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateHiringStatus hiringStatus = CandidateHiringStatus.IN_REVIEW;

    @Column(length = 1500)
    private String managerFeedback;

    private LocalDateTime hiringStatusUpdatedAt;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmploymentHistory> employmentHistory = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> educations = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resume> resumes = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateStatusHistory> statusHistory = new ArrayList<>();
}
