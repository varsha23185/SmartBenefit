package com.smartbenefit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "application_step")
@Getter @Setter @NoArgsConstructor
public class ApplicationStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id")
    private Long stepId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id")
    private Scheme scheme;

    @Column(name = "step_number") private Integer stepNumber;
    @Column(length = 1000) private String description;
    @Column(name = "portal_link", length = 500) private String portalLink;
}
