package com.smartbenefit.service;

import com.smartbenefit.model.AssessmentStatus;
import com.smartbenefit.model.Scheme;

import java.util.List;
import java.util.stream.Collectors;

/** Result of evaluating one citizen profile against one scheme (not yet saved). */
public record SchemeEvaluation(Scheme scheme, AssessmentStatus status, double matchScore, List<CriterionResult> results) {

    /** One line per criterion; this text is stored as the assessment explanation. */
    public String explanation() {
        String text = results.stream().map(CriterionResult::message).collect(Collectors.joining("\n"));
        return text.length() > 1990 ? text.substring(0, 1990) : text;
    }
}
