package com.smartbenefit.service;

import com.smartbenefit.dto.ProfileForm;
import com.smartbenefit.model.AssessmentStatus;
import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.Scheme;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * What-if simulator. It re-uses EligibilityService on a modified COPY of the profile,
 * so scenario results are always consistent with real results and the saved profile is never changed.
 */
@Service
public class WhatIfService {

    public record ScenarioChange(Scheme scheme, AssessmentStatus before, AssessmentStatus after, String explanationAfter) {
        public boolean changed() { return before != after; }
    }

    private final EligibilityService eligibilityService;

    public WhatIfService(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @Transactional(readOnly = true)
    public List<ScenarioChange> simulate(CitizenProfile real, ProfileForm scenario) {
        CitizenProfile copy = new CitizenProfile();      // transient copy - never saved
        copy.setUser(real.getUser());
        ProfileForm.from(real).applyTo(copy);
        scenario.overlayOn(copy);

        List<SchemeEvaluation> before = eligibilityService.evaluateAll(real);
        List<SchemeEvaluation> after = eligibilityService.evaluateAll(copy);

        List<ScenarioChange> changes = new ArrayList<>();
        for (int i = 0; i < before.size(); i++) {
            changes.add(new ScenarioChange(before.get(i).scheme(), before.get(i).status(),
                    after.get(i).status(), after.get(i).explanation()));
        }
        return changes;
    }
}
