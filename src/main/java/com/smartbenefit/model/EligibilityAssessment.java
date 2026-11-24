package com.smartbenefit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "eligibility_assessment")
@Getter @Setter @NoArgsConstructor
public class EligibilityAssessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    private Long assessmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id")
    private CitizenProfile citizen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id")
    private Scheme scheme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssessmentStatus status;

    @Column(name = "match_score") private Double matchScore;
    @Column(length = 2000) private String explanation;
    @Column(name = "assessed_at") private LocalDateTime assessedAt;
}
