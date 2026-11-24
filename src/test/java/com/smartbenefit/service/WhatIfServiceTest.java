package com.smartbenefit.service;

import com.smartbenefit.dto.ProfileForm;
import com.smartbenefit.model.AssessmentStatus;
import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.repository.SchemeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Automated version of the what-if test case WT-01 (Table 8.4.3). */
class WhatIfServiceTest {

    @Test @DisplayName("WT-01 Income 400,000 changes PMAY-G to Not Eligible and leaves the saved profile unchanged")
    void wt01() {
        SchemeRepository repository = Mockito.mock(SchemeRepository.class);
        Mockito.when(repository.findAllWithCriteria())
                .thenReturn(List.of(TestData.pmayg(), TestData.nrega(), TestData.kisan(), TestData.widow()));
        WhatIfService whatIf = new WhatIfService(new EligibilityService(repository));

        CitizenProfile saved = TestData.standardProfile();
        ProfileForm scenario = new ProfileForm();
        scenario.setIncome(400000.0);

        List<WhatIfService.ScenarioChange> changes = whatIf.simulate(saved, scenario);

        WhatIfService.ScenarioChange pmayg = changes.stream()
                .filter(c -> c.scheme().getName().equals("PMAY-G")).findFirst().orElseThrow();
        assertThat(pmayg.before()).isEqualTo(AssessmentStatus.ELIGIBLE);
        assertThat(pmayg.after()).isEqualTo(AssessmentStatus.NOT_ELIGIBLE);
        assertThat(pmayg.changed()).isTrue();
        assertThat(saved.getIncome()).isEqualTo(90000.0);   // the saved profile is never changed
    }

    @Test @DisplayName("A blank scenario field keeps the saved value, so nothing changes")
    void blankScenarioChangesNothing() {
        SchemeRepository repository = Mockito.mock(SchemeRepository.class);
        Mockito.when(repository.findAllWithCriteria()).thenReturn(List.of(TestData.pmayg(), TestData.kisan()));
        WhatIfService whatIf = new WhatIfService(new EligibilityService(repository));

        List<WhatIfService.ScenarioChange> changes = whatIf.simulate(TestData.standardProfile(), new ProfileForm());

        assertThat(changes).noneMatch(WhatIfService.ScenarioChange::changed);
    }
}
