package com.smartbenefit.model;

import jakarta.persistence.*;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "citizen_profile")
@Getter @Setter @NoArgsConstructor
public class CitizenProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "citizen_id")
    private Long citizenId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // All attributes are nullable: a missing value means "Additional Information Required"
    private Integer age;
    private Double income;
    @Column(name = "family_size") private Integer familySize;
    @Column(name = "education_level", length = 50) private String educationLevel;
    @Column(length = 100) private String location;
    @Column(length = 100) private String occupation;
    @Column(length = 30) private String category;
    @Column(name = "marital_status", length = 30) private String maritalStatus;
    @Column(length = 20) private String gender;

    /** The profile attributes that an eligibility criterion may refer to (used by the admin screens and the CSV import). */
    public static final List<String> PARAMETERS = List.of("age", "income", "family_size", "education_level",
            "location", "occupation", "category", "marital_status", "gender");

    /** Returns the profile value named by an eligibility criterion's "parameter" column. */
    public Object getValue(String parameter) {
        if (parameter == null) return null;
        return switch (parameter) {
            case "age" -> age;
            case "income" -> income;
            case "family_size" -> familySize;
            case "education_level" -> educationLevel;
            case "location" -> location;
            case "occupation" -> occupation;
            case "category" -> category;
            case "marital_status" -> maritalStatus;
            case "gender" -> gender;
            default -> null;
        };
    }
}
