package com.smartbenefit.service;

import com.smartbenefit.model.AssessmentStatus;
import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.Scheme;
import com.smartbenefit.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests CS-01 to CS-12: the bundled file of verified central schemes is valid, and its rules give the expected
 * result at the boundary values (the same evaluator that the application uses).
 */
class CentralSchemesDataTest {

    private static List<Scheme> schemes;
    private final EligibilityService eligibility = new EligibilityService(null);

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = CentralSchemesDataTest.class.getClassLoader().getResourceAsStream("import/central-schemes.csv")) {
            assertThat(in).as("import/central-schemes.csv is on the classpath").isNotNull();
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            SchemeImportService.ParsedFile parsed = new SchemeImportService(Mockito.mock(SchemeRepository.class)).parse(text);
            assertThat(parsed.errors()).as("errors in the bundled file").isEmpty();
            schemes = parsed.schemes();
        }
    }

    private static Scheme scheme(String namePart) {
        return schemes.stream().filter(s -> s.getName().contains(namePart)).findFirst().orElseThrow();
    }

    private static CitizenProfile profile(Integer age, Double income, String location, String occupation,
                                          String category, String gender) {
        CitizenProfile p = new CitizenProfile();
        p.setAge(age);
        p.setIncome(income);
        p.setLocation(location);
        p.setOccupation(occupation);
        p.setCategory(category);
        p.setGender(gender);
        return p;
    }

    private AssessmentStatus status(String schemeName, CitizenProfile p) {
        return eligibility.evaluateScheme(scheme(schemeName), p).status();
    }

    @Test @DisplayName("CS-01: the bundled file is valid and holds nine schemes")
    void fileIsValid() {
        assertThat(schemes).hasSize(9);
    }

    @Test @DisplayName("CS-02: every scheme has criteria, documents, steps, Tamil text, and an official source")
    void completeness() {
        for (Scheme s : schemes) {
            assertThat(s.getCriteria()).as(s.getName() + " criteria").isNotEmpty();
            assertThat(s.getDocuments()).as(s.getName() + " documents").isNotEmpty();
            assertThat(s.getSteps()).as(s.getName() + " steps").isNotEmpty();
            assertThat(s.getNameTa()).as(s.getName() + " Tamil name").isNotBlank();
            assertThat(s.getDescriptionTa()).as(s.getName() + " Tamil description").isNotBlank();
            assertThat(s.getDescription()).as(s.getName() + " source").contains("Official source: https://");
        }
    }

    @Test @DisplayName("CS-03: Ayushman Vay Vandana needs age 70 or above")
    void vayVandana() {
        assertThat(status("Vay Vandana", profile(70, null, null, null, null, null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("Vay Vandana", profile(69, null, null, null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("Vay Vandana", profile(null, null, null, null, null, null))).isEqualTo(AssessmentStatus.INFORMATION_REQUIRED);
    }

    @Test @DisplayName("CS-04: Atal Pension Yojana needs age 18 to 40")
    void atalPension() {
        assertThat(status("Atal Pension", profile(18, null, null, null, null, null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("Atal Pension", profile(40, null, null, null, null, null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("Atal Pension", profile(41, null, null, null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("Atal Pension", profile(17, null, null, null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
    }

    @Test @DisplayName("CS-05: PM-SYM needs age 18 to 40 and income up to Rs 1,80,000")
    void shramYogi() {
        assertThat(status("PM-SYM", profile(30, 180000.0, null, null, null, null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("PM-SYM", profile(30, 180001.0, null, null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("PM-SYM", profile(41, 100000.0, null, null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("PM-SYM", profile(30, null, null, null, null, null))).isEqualTo(AssessmentStatus.INFORMATION_REQUIRED);
    }

    @Test @DisplayName("CS-06: PMSBY covers age 18 to 70 and PMJJBY age 18 to 50")
    void insurance() {
        assertThat(status("PMSBY", profile(70, null, null, null, null, null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("PMSBY", profile(71, null, null, null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("PMJJBY", profile(50, null, null, null, null, null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("PMJJBY", profile(51, null, null, null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
    }

    @Test @DisplayName("CS-07: Ujjwala needs a woman aged 18 or above; a missing gender asks for information")
    void ujjwala() {
        assertThat(status("Ujjwala", profile(30, null, null, null, null, "female"))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("Ujjwala", profile(30, null, null, null, null, "male"))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("Ujjwala", profile(17, null, null, null, null, "female"))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("Ujjwala", profile(30, null, null, null, null, null))).isEqualTo(AssessmentStatus.INFORMATION_REQUIRED);
    }

    @Test @DisplayName("CS-08: a failed rule wins over missing gender (Ujjwala, age 17, gender not entered)")
    void failedRuleWinsOverMissing() {
        assertThat(status("Ujjwala", profile(17, null, null, null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
    }

    @Test @DisplayName("CS-09: the SC post-matric scholarship needs a student, category SC, and income up to Rs 2,50,000")
    void scholarship() {
        assertThat(status("Post-Matric", profile(19, 250000.0, null, "student", "SC", null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("Post-Matric", profile(19, 250001.0, null, "student", "SC", null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("Post-Matric", profile(19, 100000.0, null, "student", "GENERAL", null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("Post-Matric", profile(19, 100000.0, null, "farmer", "SC", null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
    }

    @Test @DisplayName("CS-10: PMAY-Urban 2.0 needs an urban household with income up to Rs 3,00,000")
    void urbanHousing() {
        assertThat(status("PMAY-Urban", profile(40, 300000.0, "urban", null, null, null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("PMAY-Urban", profile(40, 300001.0, "urban", null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("PMAY-Urban", profile(40, 100000.0, "rural", null, null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
    }

    @Test @DisplayName("CS-11: PM Vishwakarma needs age 18 or above and self-employment")
    void vishwakarma() {
        assertThat(status("Vishwakarma", profile(18, null, null, "self-employed", null, null))).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(status("Vishwakarma", profile(17, null, null, "self-employed", null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(status("Vishwakarma", profile(30, null, null, "farmer", null, null))).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
    }

    @Test @DisplayName("CS-12: every parameter used in the file is a real profile field")
    void parametersAreProfileFields() {
        for (Scheme s : schemes) {
            s.getCriteria().forEach(c -> assertThat(CitizenProfile.PARAMETERS).contains(c.getParameter()));
        }
    }
}
