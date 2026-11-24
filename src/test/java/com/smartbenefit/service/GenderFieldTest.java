package com.smartbenefit.service;

import com.smartbenefit.dto.ProfileForm;
import com.smartbenefit.model.CitizenProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Automated tests GN-01 to GN-04 for the gender profile field. */
class GenderFieldTest {

    @Test @DisplayName("GN-01: gender is saved from the form and read back by the rule engine")
    void savedAndReadBack() {
        ProfileForm form = new ProfileForm();
        form.setGender("female");
        CitizenProfile p = new CitizenProfile();
        form.applyTo(p);
        assertThat(p.getGender()).isEqualTo("female");
        assertThat(p.getValue("gender")).isEqualTo("female");
        assertThat(ProfileForm.from(p).getGender()).isEqualTo("female");
    }

    @Test @DisplayName("GN-02: a blank gender is stored as not entered (never as a failed condition)")
    void blankIsMissing() {
        ProfileForm form = new ProfileForm();
        form.setGender("   ");
        CitizenProfile p = new CitizenProfile();
        form.applyTo(p);
        assertThat(p.getGender()).isNull();
    }

    @Test @DisplayName("GN-03: a what-if scenario can change gender without touching the saved profile")
    void scenarioDoesNotChangeSavedProfile() {
        CitizenProfile saved = new CitizenProfile();
        saved.setGender("male");
        CitizenProfile scenario = new CitizenProfile();
        scenario.setGender(saved.getGender());
        ProfileForm change = new ProfileForm();
        change.setGender("female");
        change.overlayOn(scenario);
        assertThat(scenario.getGender()).isEqualTo("female");
        assertThat(saved.getGender()).isEqualTo("male");
    }

    @Test @DisplayName("GN-04: gender is one of the fields an administrator can use in a rule")
    void gendersIsARuleParameter() {
        assertThat(CitizenProfile.PARAMETERS).contains("gender").hasSize(9);
    }
}
