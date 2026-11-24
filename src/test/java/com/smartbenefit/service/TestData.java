package com.smartbenefit.service;

import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.CriterionOperator;
import com.smartbenefit.model.EligibilityCriterion;
import com.smartbenefit.model.Scheme;

/**
 * Test data used by the JUnit tests. The five schemes use the same benefit amounts and
 * criteria as the demo data loaded by DataInitializer, so the tests match Section 8.4.
 */
final class TestData {

    private TestData() { }

    static Scheme kisan() {
        Scheme s = scheme("PM-KISAN", 6000);
        criterion(s, "occupation", CriterionOperator.EQUALS, null, null, "farmer", true);
        criterion(s, "income", CriterionOperator.LESS_THAN_OR_EQUAL, null, 1000000.0, null, true);
        return s;
    }

    static Scheme pmayg() {
        Scheme s = scheme("PMAY-G", 120000);
        criterion(s, "location", CriterionOperator.EQUALS, null, null, "rural", true);
        criterion(s, "income", CriterionOperator.LESS_THAN_OR_EQUAL, null, 300000.0, null, true);
        criterion(s, "category", CriterionOperator.IN_LIST, null, null, "SC,ST,OBC", false);   // optional
        return s;
    }

    static Scheme sukanya() {
        Scheme s = scheme("Sukanya Samriddhi", 50000);
        criterion(s, "age", CriterionOperator.BETWEEN, 0.0, 10.0, null, true);
        return s;
    }

    static Scheme widow() {
        Scheme s = scheme("Widow Pension", 12000);
        criterion(s, "marital_status", CriterionOperator.EQUALS, null, null, "widowed", true);
        criterion(s, "age", CriterionOperator.GREATER_THAN_OR_EQUAL, 40.0, null, null, true);
        criterion(s, "income", CriterionOperator.LESS_THAN_OR_EQUAL, null, 100000.0, null, true);
        return s;
    }

    static Scheme nrega() {
        Scheme s = scheme("MGNREGA", 30000);
        criterion(s, "location", CriterionOperator.EQUALS, null, null, "rural", true);
        criterion(s, "age", CriterionOperator.GREATER_THAN_OR_EQUAL, 18.0, null, null, true);
        return s;
    }

    /** Standard test profile of Section 8.2: age 35, income 90,000, rural, farmer, SC, marital status not entered. */
    static CitizenProfile standardProfile() {
        return profile(35, 90000.0, "rural", "farmer", "SC", null);
    }

    static CitizenProfile profile(Integer age, Double income, String location, String occupation,
                                  String category, String maritalStatus) {
        CitizenProfile p = new CitizenProfile();
        p.setAge(age);
        p.setIncome(income);
        p.setLocation(location);
        p.setOccupation(occupation);
        p.setCategory(category);
        p.setMaritalStatus(maritalStatus);
        return p;
    }

    private static Scheme scheme(String name, double benefit) {
        Scheme s = new Scheme();
        s.setName(name);
        s.setBenefitAmount(benefit);
        return s;
    }

    private static void criterion(Scheme s, String parameter, CriterionOperator op, Double min, Double max,
                                  String allowed, boolean mandatory) {
        EligibilityCriterion c = new EligibilityCriterion();
        c.setScheme(s);
        c.setParameter(parameter);
        c.setOperator(op);
        c.setMinValue(min);
        c.setMaxValue(max);
        c.setAllowedValues(allowed);
        c.setMandatory(mandatory);
        s.getCriteria().add(c);
    }
}
