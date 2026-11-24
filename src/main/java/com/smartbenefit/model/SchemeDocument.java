package com.smartbenefit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scheme_document")
@Getter @Setter @NoArgsConstructor
public class SchemeDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id")
    private Scheme scheme;

    @Column(name = "document_name", nullable = false, length = 200) private String documentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", length = 30)
    private RequirementType requirementType = RequirementType.MANDATORY;
}
