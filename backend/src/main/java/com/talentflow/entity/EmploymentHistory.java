package com.talentflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "employment_history")
public class EmploymentHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String designation;

    private LocalDate startDate;
    private LocalDate endDate;
    private boolean currentlyWorking;

    @Column(length = 2000)
    private String responsibilities;

    @Column(length = 2000)
    private String technologiesUsed;
}
