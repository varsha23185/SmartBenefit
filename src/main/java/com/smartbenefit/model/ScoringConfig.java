package com.smartbenefit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "scoring_config")
@Getter @Setter @NoArgsConstructor
public class ScoringConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "w1_benefit", nullable = false) private double w1Benefit = 0.30;
    @Column(name = "w2_match", nullable = false) private double w2Match = 0.40;
    @Column(name = "w3_relevance", nullable = false) private double w3Relevance = 0.30;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at") private LocalDateTime updatedAt = LocalDateTime.now();

    /** W1 + W2 + W3 must equal 1. */
    public boolean isValid() {
        return w1Benefit >= 0 && w2Match >= 0 && w3Relevance >= 0
                && Math.abs(w1Benefit + w2Match + w3Relevance - 1.0) < 0.001;
    }
}
