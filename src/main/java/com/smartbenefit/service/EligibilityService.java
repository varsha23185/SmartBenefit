package com.smartbenefit.service;

import com.smartbenefit.model.*;
import com.smartbenefit.repository.SchemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

import com.smartbenefit.service.CriterionResult.Outcome;

/**
 * Generic, data-driven rule evaluator. No scheme-specific code exists here:
 * every rule is a row in eligibility_criterion.
 * The same methods are used by the normal eligibility check AND by the what-if simulator.
 */
@Service
public class EligibilityService {

    private final SchemeRepository schemeRepository;

    public EligibilityService(SchemeRepository schemeRepository) {
        this.schemeRepository = schemeRepository;
    }

    /** Evaluates the profile against every scheme (nothing is saved). */
    @Transactional(readOnly = true)
    public List<SchemeEvaluation> evaluateAll(CitizenProfile profile) {
        return schemeRepository.findAllWithCriteria().stream()
                .map(s -> evaluateScheme(s, profile))
                .toList();
    }

    public SchemeEvaluation evaluateScheme(Scheme scheme, CitizenProfile profile) {
        List<CriterionResult> results = scheme.getCriteria().stream()
                .map(c -> evaluate(c, profile))
                .toList();
        return new SchemeEvaluation(scheme, decide(results), matchScore(results), results);
    }

    /** A failed mandatory criterion wins over missing information. */
    public AssessmentStatus decide(List<CriterionResult> results) {
        if (results.stream().anyMatch(r -> r.isMandatory() && r.isNotMet())) return AssessmentStatus.NOT_ELIGIBLE;
        if (results.stream().anyMatch(r -> r.isMandatory() && r.isMissing())) return AssessmentStatus.INFORMATION_REQUIRED;
        return AssessmentStatus.ELIGIBLE;
    }

    /** Average "comfort" of the criteria that could be evaluated (0 to 1). */
    public double matchScore(List<CriterionResult> results) {
        return results.stream().filter(r -> !r.isMissing())
                .mapToDouble(CriterionResult::comfort).average().orElse(0.0);
    }

    public CriterionResult evaluate(EligibilityCriterion c, CitizenProfile p) {
        String label = label(c.getParameter());
        Object value = p.getValue(c.getParameter());
        if (value == null || (value instanceof String s && s.isBlank())) {
            return new CriterionResult(c, Outcome.MISSING, 0, "? " + label + ": please provide this information (" + describe(c) + ")");
        }

        double lo = c.getMinValue() == null ? Double.NEGATIVE_INFINITY : c.getMinValue();
        double hi = c.getMaxValue() == null ? Double.POSITIVE_INFINITY : c.getMaxValue();
        boolean met;
        double comfort = 0.0;

        switch (c.getOperator()) {
            case BETWEEN -> {
                double v = number(value);
                met = v >= lo && v <= hi;
                if (met) {
                    double half = (hi - lo) / 2.0;
                    comfort = Double.isFinite(half) && half > 0
                            ? 0.5 + 0.5 * Math.min(1.0, Math.min(v - lo, hi - v) / half) : 1.0;
                }
            }
            case LESS_THAN_OR_EQUAL -> {
                double v = number(value);
                met = v <= hi;
                if (met) comfort = Double.isFinite(hi) && hi > 0 ? 0.5 + 0.5 * Math.min(1.0, (hi - v) / hi) : 1.0;
            }
            case GREATER_THAN_OR_EQUAL -> {
                double v = number(value);
                met = v >= lo;
                if (met) comfort = Double.isFinite(lo) && lo > 0 ? 0.5 + 0.5 * Math.min(1.0, (v - lo) / lo) : 1.0;
            }
            default -> { // EQUALS, IN_LIST
                met = c.allowedList().contains(value.toString().trim().toLowerCase(Locale.ROOT));
                comfort = met ? 1.0 : 0.0;
            }
        }

        String text = label + ": " + value + (met ? " meets" : " does not meet") + " the condition (" + describe(c) + ")";
        return new CriterionResult(c, met ? Outcome.MET : Outcome.NOT_MET, comfort,
                (met ? "\u2713 " : "\u2717 ") + text + (c.isMandatory() ? "" : " [optional]"));
    }

    private double number(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return Double.NaN; }
    }

    static String describe(EligibilityCriterion c) {
        return switch (c.getOperator()) {
            case BETWEEN -> "between " + fmt(c.getMinValue()) + " and " + fmt(c.getMaxValue());
            case LESS_THAN_OR_EQUAL -> "at most " + fmt(c.getMaxValue());
            case GREATER_THAN_OR_EQUAL -> "at least " + fmt(c.getMinValue());
            default -> "one of: " + (c.getAllowedValues() == null ? "-" : c.getAllowedValues());
        };
    }

    private static String fmt(Double d) {
        if (d == null) return "-";
        return d == Math.rint(d) ? String.valueOf(d.longValue()) : String.valueOf(d);
    }

    static String label(String parameter) {
        String s = parameter.replace('_', ' ');
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
