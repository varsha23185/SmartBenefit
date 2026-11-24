package com.smartbenefit.service;

import com.smartbenefit.model.*;
import com.smartbenefit.repository.EligibilityAssessmentRepository;
import com.smartbenefit.repository.RecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Runs a full check (evaluate -> store assessments -> rank -> store recommendations) in ONE transaction. */
@Service
public class EligibilityCheckService {

    private final EligibilityService eligibilityService;
    private final RecommendationService recommendationService;
    private final EligibilityAssessmentRepository assessmentRepository;
    private final RecommendationRepository recommendationRepository;

    public EligibilityCheckService(EligibilityService eligibilityService, RecommendationService recommendationService,
                                   EligibilityAssessmentRepository assessmentRepository,
                                   RecommendationRepository recommendationRepository) {
        this.eligibilityService = eligibilityService;
        this.recommendationService = recommendationService;
        this.assessmentRepository = assessmentRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public void check(CitizenProfile profile) {
        List<SchemeEvaluation> evaluations = eligibilityService.evaluateAll(profile);
        LocalDateTime now = LocalDateTime.now();

        // Old recommendations are replaced; old assessments are kept as history.
        recommendationRepository.deleteByCitizen(profile);
        recommendationRepository.flush();

        List<RecommendationService.RankedScheme> ranked = recommendationService.rank(evaluations, profile);
        java.util.Map<Long, String> rankingText = new java.util.HashMap<>();
        for (RecommendationService.RankedScheme r : ranked) {
            rankingText.put(r.evaluation().scheme().getSchemeId(),
                    String.format("%nRanking: #%d, score %.3f (%s)", r.rank(), r.score(), r.reason()));
        }

        java.util.Map<Long, EligibilityAssessment> saved = new java.util.HashMap<>();
        for (SchemeEvaluation e : evaluations) {
            EligibilityAssessment a = new EligibilityAssessment();
            a.setCitizen(profile);
            a.setScheme(e.scheme());
            a.setStatus(e.status());
            a.setMatchScore(e.matchScore());
            String text = e.explanation() + rankingText.getOrDefault(e.scheme().getSchemeId(), "");
            a.setExplanation(text.length() > 2000 ? text.substring(0, 2000) : text);
            a.setAssessedAt(now);
            saved.put(e.scheme().getSchemeId(), assessmentRepository.save(a));
        }

        for (RecommendationService.RankedScheme r : ranked) {
            Recommendation rec = new Recommendation();
            rec.setAssessment(saved.get(r.evaluation().scheme().getSchemeId()));
            rec.setCitizen(profile);
            rec.setScheme(r.evaluation().scheme());
            rec.setScore(r.score());
            rec.setRankPosition(r.rank());
            recommendationRepository.save(rec);
        }
    }
}
