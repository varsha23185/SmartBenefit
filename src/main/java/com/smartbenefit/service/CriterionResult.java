package com.smartbenefit.service;

import com.smartbenefit.model.EligibilityCriterion;

/** Outcome of checking ONE criterion against a citizen profile. */
public record CriterionResult(EligibilityCriterion criterion, Outcome outcome, double comfort, String message) {

    public enum Outcome { MET, NOT_MET, MISSING }

    public boolean isMandatory() { return criterion.isMandatory(); }
    public boolean isMet() { return outcome == Outcome.MET; }
    public boolean isNotMet() { return outcome == Outcome.NOT_MET; }
    public boolean isMissing() { return outcome == Outcome.MISSING; }
}
