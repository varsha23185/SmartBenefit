package com.smartbenefit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scheme")
@Getter @Setter @NoArgsConstructor
public class Scheme {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scheme_id")
    private Long schemeId;

    @Column(nullable = false, length = 200) private String name;
    @Column(name = "name_ta", length = 300) private String nameTa;
    @Column(length = 1000) private String description;
    @Column(name = "description_ta", length = 2000) private String descriptionTa;
    @Column(name = "benefit_type", length = 50) private String benefitType;
    @Column(name = "benefit_amount") private Double benefitAmount = 0.0;
    @Column(length = 100) private String category;

    /** Number of times citizens opened the application guidance page of this scheme (shown to the administrator). */
    @Column(name = "view_count", nullable = false, columnDefinition = "bigint not null default 0")
    private long viewCount = 0;

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EligibilityCriterion> criteria = new ArrayList<>();

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("documentId ASC")
    private List<SchemeDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    private List<ApplicationStep> steps = new ArrayList<>();

    /** Tamil name when the interface language is Tamil and a Tamil value exists; English otherwise. */
    public String localName(String lang) {
        return "ta".equals(lang) && nameTa != null && !nameTa.isBlank() ? nameTa : name;
    }

    public String localDescription(String lang) {
        return "ta".equals(lang) && descriptionTa != null && !descriptionTa.isBlank() ? descriptionTa : description;
    }
}
