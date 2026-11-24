package com.smartbenefit.service;

import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.ScoringConfig;
import com.smartbenefit.repository.ScoringConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Automated version of the ranking test cases RT-01 to RT-05 (Table 8.4.2). */
class RecommendationServiceTest {

    private final EligibilityService eligibility = new EligibilityService(null);

    private RecommendationService serviceWithWeights(double w1, double w2, double w3) {
        ScoringConfig config = new ScoringConfig();
        config.setW1Benefit(w1);
        config.setW2Match(w2);
        config.setW3Relevance(w3);
        ScoringConfigRepository repository = Mockito.mock(ScoringConfigRepository.class);
        Mockito.when(repository.findTopByOrderByConfigIdDesc()).thenReturn(Optional.of(config));
        return new RecommendationService(repository);
    }

    private List<SchemeEvaluation> evaluateAll(CitizenProfile profile) {
        return List.of(TestData.pmayg(), TestData.nrega(), TestData.kisan(), TestData.widow(), TestData.sukanya())
                .stream().map(s -> eligibility.evaluateScheme(s, profile)).toList();
    }

    private double scoreOf(List<RecommendationService.RankedScheme> ranked, String schemeName) {
        return ranked.stream().filter(r -> r.evaluation().scheme().getName().equals(schemeName))
                .findFirst().orElseThrow().score();
    }

    private List<String> order(List<RecommendationService.RankedScheme> ranked) {
        return ranked.stream().map(r -> r.evaluation().scheme().getName()).toList();
    }

    @Test @DisplayName("RT-01 Default weights: PMAY-G first, PM-KISAN last")
    void rt01() {
        CitizenProfile p = TestData.standardProfile();
        var ranked = serviceWithWeights(0.30, 0.40, 0.30).rank(evaluateAll(p), p);
        assertThat(order(ranked)).containsExactly("PMAY-G", "MGNREGA", "PM-KISAN");
        assertThat(scoreOf(ranked, "PMAY-G")).isCloseTo(0.880, within(0.001));
        assertThat(scoreOf(ranked, "MGNREGA")).isCloseTo(0.569, within(0.001));
        assertThat(scoreOf(ranked, "PM-KISAN")).isCloseTo(0.506, within(0.001));
        assertThat(ranked).extracting(RecommendationService.RankedScheme::rank).containsExactly(1, 2, 3);
    }

    @Test @DisplayName("RT-02 Weights 0 / 1 / 0: the order follows the match score")
    void rt02() {
        CitizenProfile p = TestData.standardProfile();
        var ranked = serviceWithWeights(0, 1, 0).rank(evaluateAll(p), p);
        assertThat(order(ranked)).containsExactly("MGNREGA", "PM-KISAN", "PMAY-G");
        assertThat(scoreOf(ranked, "MGNREGA")).isCloseTo(0.986, within(0.001));
        assertThat(scoreOf(ranked, "PM-KISAN")).isCloseTo(0.978, within(0.001));
        assertThat(scoreOf(ranked, "PMAY-G")).isCloseTo(0.950, within(0.001));
    }

    @Test @DisplayName("RT-03 Weights 1 / 0 / 0: the score equals the normalized benefit")
    void rt03() {
        CitizenProfile p = TestData.standardProfile();
        var ranked = serviceWithWeights(1, 0, 0).rank(evaluateAll(p), p);
        assertThat(order(ranked)).containsExactly("PMAY-G", "MGNREGA", "PM-KISAN");
        assertThat(scoreOf(ranked, "PMAY-G")).isCloseTo(1.000, within(0.001));
        assertThat(scoreOf(ranked, "MGNREGA")).isCloseTo(0.250, within(0.001));
        assertThat(scoreOf(ranked, "PM-KISAN")).isCloseTo(0.050, within(0.001));
    }

    @Test @DisplayName("RT-04 Empty profile: no scheme is ranked")
    void rt04() {
        CitizenProfile p = new CitizenProfile();
        assertThat(serviceWithWeights(0.30, 0.40, 0.30).rank(evaluateAll(p), p)).isEmpty();
    }

    @Test @DisplayName("RT-05 Ineligible schemes never appear in the ranking")
    void rt05() {
        CitizenProfile p = TestData.standardProfile();
        var ranked = serviceWithWeights(0.30, 0.40, 0.30).rank(evaluateAll(p), p);
        assertThat(order(ranked)).doesNotContain("Widow Pension", "Sukanya Samriddhi");
    }

    @Test @DisplayName("Profile relevance counts targeted attributes the citizen matches (PMAY-G: 2 of 3)")
    void relevance() {
        CitizenProfile p = TestData.standardProfile();
        RecommendationService service = serviceWithWeights(0.30, 0.40, 0.30);
        assertThat(service.profileRelevance(eligibility.evaluateScheme(TestData.pmayg(), p), p)).isCloseTo(2.0 / 3, within(0.001));
        assertThat(service.profileRelevance(eligibility.evaluateScheme(TestData.nrega(), p), p)).isCloseTo(1.0 / 3, within(0.001));
    }

    @Test @DisplayName("Ranking weights must be non-negative and add up to 1")
    void weightValidation() {
        ScoringConfig ok = new ScoringConfig();
        assertThat(ok.isValid()).isTrue();                 // default 0.30 / 0.40 / 0.30
        ok.setW1Benefit(0.5); ok.setW2Match(0.5); ok.setW3Relevance(0.5);
        assertThat(ok.isValid()).isFalse();                // sums to 1.5
        ok.setW1Benefit(1.2); ok.setW2Match(-0.1); ok.setW3Relevance(-0.1);
        assertThat(ok.isValid()).isFalse();                // sums to 1 but has negative weights
    }
}
