package com.smartbenefit.service;

import com.smartbenefit.model.*;
import com.smartbenefit.repository.ScoringConfigRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Ranks ELIGIBLE schemes with the transparent weighted formula:
 * Score = W1 x NormalizedBenefit + W2 x EligibilityMatch + W3 x ProfileRelevance
 * Weights are read from scoring_config, so an administrator can change them without code changes.
 */
@Service
public class RecommendationService {

    /** Profile attributes that a scheme can "target" (used for the relevance score). */
    private static final Set<String> TARGETING = Set.of("occupation", "category", "location", "marital_status", "gender");

    private final ScoringConfigRepository scoringConfigRepository;

    public RecommendationService(ScoringConfigRepository scoringConfigRepository) {
        this.scoringConfigRepository = scoringConfigRepository;
    }

    public record RankedScheme(SchemeEvaluation evaluation, double benefitScore, double relevanceScore,
                               double score, int rank, String reason) { }

    public ScoringConfig currentWeights() {
        return scoringConfigRepository.findTopByOrderByConfigIdDesc().orElseGet(ScoringConfig::new);
    }

    public List<RankedScheme> rank(List<SchemeEvaluation> evaluations, CitizenProfile profile) {
        ScoringConfig w = currentWeights();
        List<SchemeEvaluation> eligible = evaluations.stream()
                .filter(e -> e.status() == AssessmentStatus.ELIGIBLE).toList();

        double maxBenefit = eligible.stream()
                .mapToDouble(e -> e.scheme().getBenefitAmount() == null ? 0 : e.scheme().getBenefitAmount())
                .max().orElse(0);
        if (maxBenefit <= 0) maxBenefit = 1;

        List<RankedScheme> unsorted = new ArrayList<>();
        for (SchemeEvaluation e : eligible) {
            double benefit = (e.scheme().getBenefitAmount() == null ? 0 : e.scheme().getBenefitAmount()) / maxBenefit;
            double match = e.matchScore();
            double relevance = profileRelevance(e, profile);
            double score = w.getW1Benefit() * benefit + w.getW2Match() * match + w.getW3Relevance() * relevance;
            String reason = String.format("Benefit %.2f x %.2f + Match %.2f x %.2f + Relevance %.2f x %.2f",
                    benefit, w.getW1Benefit(), match, w.getW2Match(), relevance, w.getW3Relevance());
            unsorted.add(new RankedScheme(e, benefit, relevance, score, 0, reason));
        }

        unsorted.sort(Comparator.comparingDouble(RankedScheme::score).reversed());
        List<RankedScheme> ranked = new ArrayList<>();
        for (int i = 0; i < unsorted.size(); i++) {
            RankedScheme r = unsorted.get(i);
            ranked.add(new RankedScheme(r.evaluation(), r.benefitScore(), r.relevanceScore(), r.score(), i + 1, r.reason()));
        }
        return ranked;
    }

    /**
     * Share of the citizen's entered targeting attributes (occupation, category, location, marital status, gender)
     * that the scheme specifically targets and the citizen matches.
     */
    public double profileRelevance(SchemeEvaluation e, CitizenProfile p) {
        long entered = TARGETING.stream().filter(param -> {
            Object v = p.getValue(param);
            return v != null && !v.toString().isBlank();
        }).count();
        if (entered == 0) return 0.0;
        long matched = e.results().stream()
                .filter(r -> r.isMet() && TARGETING.contains(r.criterion().getParameter()))
                .map(r -> r.criterion().getParameter()).distinct().count();
        return (double) matched / entered;
    }
}
