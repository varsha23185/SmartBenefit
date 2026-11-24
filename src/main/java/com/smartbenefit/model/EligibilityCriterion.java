package com.smartbenefit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "eligibility_criterion")
@Getter @Setter @NoArgsConstructor
public class EligibilityCriterion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criterion_id")
    private Long criterionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id")
    private Scheme scheme;

    @Column(nullable = false, length = 50) private String parameter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CriterionOperator operator;

    @Column(name = "min_value") private Double minValue;
    @Column(name = "max_value") private Double maxValue;
    @Column(name = "allowed_values", length = 255) private String allowedValues;
    @Column(name = "is_mandatory", nullable = false) private boolean mandatory = true;

    /** Comma-separated allowed_values as a lower-case list. */
    public List<String> allowedList() {
        if (allowedValues == null || allowedValues.isBlank()) return List.of();
        return Arrays.stream(allowedValues.split(",")).map(s -> s.trim().toLowerCase()).filter(s -> !s.isEmpty()).toList();
    }
}
