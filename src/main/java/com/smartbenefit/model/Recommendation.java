package com.smartbenefit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recommendation")
@Getter @Setter @NoArgsConstructor
public class Recommendation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", unique = true)
    private EligibilityAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id")
    private CitizenProfile citizen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id")
    private Scheme scheme;

    private Double score;
    @Column(name = "rank_position") private Integer rankPosition;
}
