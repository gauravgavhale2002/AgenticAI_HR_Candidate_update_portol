package com.talentflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_recommendations")
public class AIRecommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AIAnalysis analysis;

    @Column(nullable = false)
    private String recommendationType;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(length = 2000)
    private String rationale;
}
