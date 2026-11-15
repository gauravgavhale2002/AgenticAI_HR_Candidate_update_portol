package com.talentflow.entity;

import com.talentflow.enums.EmploymentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    private String location;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    private Integer minimumExperienceYears;
    private Integer maximumExperienceYears;

    @Column(precision = 18, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 18, scale = 2)
    private BigDecimal salaryMax;

    @Column(length = 4000)
    private String description;

    @Column(length = 4000)
    private String requirements;

    private boolean active = true;
    private LocalDate closingDate;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobSkillRequirement> skillRequirements = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobApplication> applications = new ArrayList<>();
}
