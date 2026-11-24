package com.smartbenefit.service;

import com.smartbenefit.model.AssessmentStatus;
import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.Scheme;
import com.smartbenefit.service.CriterionResult.Outcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.smartbenefit.model.AssessmentStatus.ELIGIBLE;
import static com.smartbenefit.model.AssessmentStatus.INFORMATION_REQUIRED;
import static com.smartbenefit.model.AssessmentStatus.NOT_ELIGIBLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Automated version of the rule engine test cases ET-01 to ET-14 (Table 8.4.1). */
class EligibilityServiceTest {

    // evaluateScheme() does not use the repository, so no database is needed
    private final EligibilityService service = new EligibilityService(null);

    private AssessmentStatus status(Scheme scheme, CitizenProfile profile) {
        return service.evaluateScheme(scheme, profile).status();
    }

    private CitizenProfile profile(Integer age, Double income, String location, String occupation,
                                   String category, String marital) {
        return TestData.profile(age, income, location, occupation, category, marital);
    }

    @Test @DisplayName("ET-01 PMAY-G: income exactly at the limit is eligible")
    void et01() { assertThat(status(TestData.pmayg(), profile(null, 300000.0, "rural", null, null, null))).isEqualTo(ELIGIBLE); }

    @Test @DisplayName("ET-02 PMAY-G: income just above the limit is not eligible")
    void et02() { assertThat(status(TestData.pmayg(), profile(null, 300001.0, "rural", null, null, null))).isEqualTo(NOT_ELIGIBLE); }

    @Test @DisplayName("ET-03 Sukanya: age 10 (upper boundary) is eligible")
    void et03() { assertThat(status(TestData.sukanya(), profile(10, null, null, null, null, null))).isEqualTo(ELIGIBLE); }

    @Test @DisplayName("ET-04 Sukanya: age 11 is not eligible")
    void et04() { assertThat(status(TestData.sukanya(), profile(11, null, null, null, null, null))).isEqualTo(NOT_ELIGIBLE); }

    @Test @DisplayName("ET-05 Sukanya: age 0 (lower boundary) is eligible")
    void et05() { assertThat(status(TestData.sukanya(), profile(0, null, null, null, null, null))).isEqualTo(ELIGIBLE); }

    @Test @DisplayName("ET-06 Empty profile: information required for PMAY-G, PM-KISAN and Widow Pension")
    void et06() {
        CitizenProfile empty = new CitizenProfile();
        assertThat(status(TestData.pmayg(), empty)).isEqualTo(INFORMATION_REQUIRED);
        assertThat(status(TestData.kisan(), empty)).isEqualTo(INFORMATION_REQUIRED);
        assertThat(status(TestData.widow(), empty)).isEqualTo(INFORMATION_REQUIRED);
    }

    @Test @DisplayName("ET-07 PM-KISAN: farmer with income not entered needs information")
    void et07() { assertThat(status(TestData.kisan(), profile(null, null, null, "farmer", null, null))).isEqualTo(INFORMATION_REQUIRED); }

    @Test @DisplayName("ET-08 PM-KISAN: student is not eligible even though income is missing")
    void et08() { assertThat(status(TestData.kisan(), profile(null, null, null, "student", null, null))).isEqualTo(NOT_ELIGIBLE); }

    @Test @DisplayName("ET-09 PM-KISAN: comparison ignores letter case")
    void et09() { assertThat(status(TestData.kisan(), profile(null, 50000.0, null, "FARMER", null, null))).isEqualTo(ELIGIBLE); }

    @Test @DisplayName("ET-10 PMAY-G: unmet optional criterion keeps the status but lowers the match score")
    void et10() {
        SchemeEvaluation general = service.evaluateScheme(TestData.pmayg(), profile(null, 100000.0, "rural", null, "General", null));
        SchemeEvaluation sc = service.evaluateScheme(TestData.pmayg(), profile(null, 100000.0, "rural", null, "SC", null));
        assertThat(general.status()).isEqualTo(ELIGIBLE);
        assertThat(sc.status()).isEqualTo(ELIGIBLE);
        assertThat(general.matchScore()).isCloseTo(0.61, within(0.006));
        assertThat(sc.matchScore()).isCloseTo(0.94, within(0.006));
        assertThat(general.matchScore()).isLessThan(sc.matchScore());
    }

    @Test @DisplayName("ET-11 Widow Pension: age fails first, so not eligible")
    void et11() { assertThat(status(TestData.widow(), profile(35, 50000.0, null, null, null, null))).isEqualTo(NOT_ELIGIBLE); }

    @Test @DisplayName("ET-12 Widow Pension: age 45 and marital status missing needs information")
    void et12() { assertThat(status(TestData.widow(), profile(45, 50000.0, null, null, null, null))).isEqualTo(INFORMATION_REQUIRED); }

    @Test @DisplayName("ET-13 Widow Pension: age 45, income 50,000, widowed is eligible")
    void et13() { assertThat(status(TestData.widow(), profile(45, 50000.0, null, null, null, "widowed"))).isEqualTo(ELIGIBLE); }

    @Test @DisplayName("ET-14 Widow Pension: income 100,001 is not eligible")
    void et14() { assertThat(status(TestData.widow(), profile(45, 100001.0, null, null, null, "widowed"))).isEqualTo(NOT_ELIGIBLE); }

    @Test @DisplayName("D-01 A failed mandatory criterion wins over a missing mandatory criterion")
    void failedMandatoryWinsOverMissing() {
        var failed = service.evaluate(TestData.sukanya().getCriteria().get(0), profile(15, null, null, null, null, null));
        var missing = service.evaluate(TestData.kisan().getCriteria().get(0), new CitizenProfile());
        assertThat(failed.outcome()).isEqualTo(Outcome.NOT_MET);
        assertThat(missing.outcome()).isEqualTo(Outcome.MISSING);
        assertThat(service.decide(List.of(failed, missing))).isEqualTo(NOT_ELIGIBLE);
        assertThat(service.decide(List.of(missing))).isEqualTo(INFORMATION_REQUIRED);
    }

    @Test @DisplayName("A scheme with no criteria is eligible")
    void noCriteria() { assertThat(status(new Scheme(), new CitizenProfile())).isEqualTo(ELIGIBLE); }
}
