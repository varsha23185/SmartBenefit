package com.smartbenefit.service;

import com.smartbenefit.model.CriterionOperator;
import com.smartbenefit.model.EligibilityCriterion;
import com.smartbenefit.model.Scheme;
import com.smartbenefit.repository.SchemeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Automated tests IM-01 to IM-12 for the CSV import (administrator function). */
class SchemeImportServiceTest {

    private static final String HEADER = "type,scheme,benefit_amount,description,parameter,operator,min_value,max_value,"
            + "allowed_values,mandatory,document_name,requirement_type,step_number,step_text,portal_link";

    private final SchemeRepository repository = Mockito.mock(SchemeRepository.class);
    private final SchemeImportService service = new SchemeImportService(repository);

    private static String q(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Builds one CSV row with the 15 columns of HEADER. */
    private static String row(String... cells) {
        String[] all = new String[15];
        for (int i = 0; i < 15; i++) all[i] = i < cells.length ? q(cells[i]) : "";
        return String.join(",", all);
    }

    private static String file(String... rows) {
        return HEADER + "\n" + String.join("\n", rows) + "\n";
    }

    private static final String SCHEME_ROW = row("SCHEME", "Test Scheme", "5000", "A test scheme");
    private static final String CRITERION_ROW = row("CRITERION", "Test Scheme", "", "", "age", "GREATER_THAN_OR_EQUAL", "18", "", "", "true");

    @Test @DisplayName("IM-01: a valid file adds the scheme with its criteria, documents, and steps")
    void validFile() {
        String csv = file(SCHEME_ROW, CRITERION_ROW,
                row("DOCUMENT", "Test Scheme", "", "", "", "", "", "", "", "", "Aadhaar card", "MANDATORY"),
                row("STEP", "Test Scheme", "", "", "", "", "", "", "", "", "", "", "1", "Apply online", "https://example.gov.in/"));
        SchemeImportService.ImportResult result = service.importText(csv);
        assertThat(result.errors()).isEmpty();
        assertThat(result.schemesAdded()).isEqualTo(1);
        assertThat(result.criteria()).isEqualTo(1);
        assertThat(result.documents()).isEqualTo(1);
        assertThat(result.steps()).isEqualTo(1);
        verify(repository, times(1)).save(any(Scheme.class));
    }

    @Test @DisplayName("IM-02: commas and quotes inside a quoted value are kept")
    void quotedValues() {
        String csv = file(row("SCHEME", "Test Scheme", "5000", "Pension, \"assured\" for workers"), CRITERION_ROW);
        SchemeImportService.ParsedFile parsed = service.parse(csv);
        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.schemes().get(0).getDescription()).isEqualTo("Pension, \"assured\" for workers");
    }

    @Test @DisplayName("IM-03: an unknown profile parameter is rejected and nothing is saved")
    void unknownParameter() {
        String csv = file(SCHEME_ROW, row("CRITERION", "Test Scheme", "", "", "religion", "EQUALS", "", "", "x", "true"));
        SchemeImportService.ImportResult result = service.importText(csv);
        assertThat(result.errors()).anyMatch(e -> e.contains("Row 3") && e.contains("religion"));
        assertThat(result.schemesAdded()).isZero();
        verify(repository, never()).save(any(Scheme.class));
    }

    @Test @DisplayName("IM-04: BETWEEN without a maximum is rejected")
    void betweenNeedsBothLimits() {
        String csv = file(SCHEME_ROW, row("CRITERION", "Test Scheme", "", "", "age", "BETWEEN", "18", "", "", "true"));
        assertThat(service.parse(csv).errors()).isNotEmpty();
    }

    @Test @DisplayName("IM-05: a step link that is not http or https is rejected")
    void unsafeLink() {
        String csv = file(SCHEME_ROW, CRITERION_ROW,
                row("STEP", "Test Scheme", "", "", "", "", "", "", "", "", "", "", "1", "test", "javascript:alert(1)"));
        assertThat(service.parse(csv).errors()).anyMatch(e -> e.contains("step"));
    }

    @Test @DisplayName("IM-06: a scheme that already exists is skipped, not duplicated")
    void existingSchemeSkipped() {
        when(repository.existsByNameIgnoreCase("Test Scheme")).thenReturn(true);
        SchemeImportService.ImportResult result = service.importText(file(SCHEME_ROW, CRITERION_ROW));
        assertThat(result.schemesAdded()).isZero();
        assertThat(result.skipped()).containsExactly("Test Scheme");
        verify(repository, never()).save(any(Scheme.class));
    }

    @Test @DisplayName("IM-07: a scheme without any criterion is rejected")
    void schemeNeedsCriterion() {
        SchemeImportService.ParsedFile parsed = service.parse(file(SCHEME_ROW));
        assertThat(parsed.errors()).anyMatch(e -> e.contains("no CRITERION"));
    }

    @Test @DisplayName("IM-08: a criterion placed before its scheme is rejected")
    void criterionBeforeScheme() {
        String csv = file(CRITERION_ROW, SCHEME_ROW);
        assertThat(service.parse(csv).errors()).anyMatch(e -> e.contains("Row 2") && e.contains("not been defined"));
    }

    @Test @DisplayName("IM-09: a numeric operator on a text parameter is rejected")
    void numericOperatorOnText() {
        String csv = file(SCHEME_ROW, row("CRITERION", "Test Scheme", "", "", "occupation", "LESS_THAN_OR_EQUAL", "", "10", "", "true"));
        assertThat(service.parse(csv).errors()).anyMatch(e -> e.contains("occupation"));
    }

    @Test @DisplayName("IM-10: Tamil text and a byte-order mark (Excel) are handled")
    void tamilAndBom() {
        String csv = "\uFEFF" + HEADER.replace("description", "name_ta") + "\n"
                + row("SCHEME", "Test Scheme", "5000", "அடல் ஓய்வூதியத் திட்டம்") + "\n" + CRITERION_ROW + "\n";
        SchemeImportService.ParsedFile parsed = service.parse(csv);
        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.schemes().get(0).getNameTa()).isEqualTo("அடல் ஓய்வூதியத் திட்டம்");
    }

    @Test @DisplayName("IM-11: a negative benefit amount is rejected")
    void negativeBenefit() {
        String csv = file(row("SCHEME", "Test Scheme", "-5"), CRITERION_ROW);
        assertThat(service.parse(csv).errors()).anyMatch(e -> e.contains("benefit_amount"));
    }

    @Test @DisplayName("IM-12: the mandatory column accepts yes and no, and defaults to mandatory")
    void mandatoryColumn() {
        String csv = file(SCHEME_ROW,
                row("CRITERION", "Test Scheme", "", "", "age", "GREATER_THAN_OR_EQUAL", "18", "", "", "no"),
                row("CRITERION", "Test Scheme", "", "", "income", "LESS_THAN_OR_EQUAL", "", "100000", "", ""));
        List<EligibilityCriterion> criteria = service.parse(csv).schemes().get(0).getCriteria();
        assertThat(criteria.get(0).isMandatory()).isFalse();
        assertThat(criteria.get(1).isMandatory()).isTrue();
        assertThat(criteria.get(1).getOperator()).isEqualTo(CriterionOperator.LESS_THAN_OR_EQUAL);
    }
}
