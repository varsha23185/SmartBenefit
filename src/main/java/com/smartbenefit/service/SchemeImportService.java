package com.smartbenefit.service;

import com.smartbenefit.model.ApplicationStep;
import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.CriterionOperator;
import com.smartbenefit.model.EligibilityCriterion;
import com.smartbenefit.model.RequirementType;
import com.smartbenefit.model.Scheme;
import com.smartbenefit.model.SchemeDocument;
import com.smartbenefit.repository.SchemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bulk import of schemes from a CSV file (administrator function).
 *
 * One file holds four kinds of rows, told apart by the "type" column:
 * SCHEME, CRITERION, DOCUMENT and STEP. A CRITERION, DOCUMENT or STEP row belongs to the scheme named in
 * its "scheme" column, which must be defined by a SCHEME row earlier in the file.
 *
 * The whole file is validated first. If even one row is invalid, NOTHING is saved and every problem is
 * reported with its row number, so the administrator can correct the file and upload it again.
 * A scheme whose name already exists is skipped, so uploading the same file twice does not create duplicates.
 */
@Service
public class SchemeImportService {

    public static final int MAX_ROWS = 2000;
    private static final Set<String> NUMERIC_PARAMETERS = Set.of("age", "income", "family_size");

    /** Result of reading and checking a file; nothing is saved at this stage. */
    public record ParsedFile(List<Scheme> schemes, List<String> errors, int criteria, int documents, int steps) { }

    /** Result of an import run. */
    public record ImportResult(int schemesAdded, List<String> skipped, int criteria, int documents, int steps,
                               List<String> errors) {
        public boolean success() { return errors.isEmpty(); }
    }

    private final SchemeRepository schemeRepository;

    public SchemeImportService(SchemeRepository schemeRepository) {
        this.schemeRepository = schemeRepository;
    }

    @Transactional
    public ImportResult importCsv(InputStream in) throws IOException {
        return importText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Transactional
    public ImportResult importText(String text) {
        ParsedFile parsed = parse(text);
        if (!parsed.errors().isEmpty()) {
            return new ImportResult(0, List.of(), 0, 0, 0, parsed.errors());      // all or nothing
        }
        int added = 0;
        int criteria = 0;
        int documents = 0;
        int steps = 0;
        List<String> skipped = new ArrayList<>();
        for (Scheme s : parsed.schemes()) {
            if (schemeRepository.existsByNameIgnoreCase(s.getName())) {
                skipped.add(s.getName());
                continue;
            }
            schemeRepository.save(s);
            added++;
            criteria += s.getCriteria().size();
            documents += s.getDocuments().size();
            steps += s.getSteps().size();
        }
        return new ImportResult(added, skipped, criteria, documents, steps, List.of());
    }

    /** Reads and validates the text of a CSV file without saving anything. */
    ParsedFile parse(String rawText) {
        List<String> errors = new ArrayList<>();
        String text = rawText.startsWith("\uFEFF") ? rawText.substring(1) : rawText;     // Excel adds a BOM

        List<List<String>> rows;
        try {
            rows = readCsv(text);
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
            return new ParsedFile(List.of(), errors, 0, 0, 0);
        }
        if (rows.isEmpty()) {
            errors.add("The file is empty.");
            return new ParsedFile(List.of(), errors, 0, 0, 0);
        }

        Map<String, Integer> col = new HashMap<>();
        List<String> header = rows.get(0);
        for (int i = 0; i < header.size(); i++) {
            col.put(header.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        if (!col.containsKey("type") || !col.containsKey("scheme")) {
            errors.add("The first row must be a header that contains at least the columns \"type\" and \"scheme\".");
            return new ParsedFile(List.of(), errors, 0, 0, 0);
        }
        if (rows.size() - 1 > MAX_ROWS) {
            errors.add("The file has more than " + MAX_ROWS + " rows.");
            return new ParsedFile(List.of(), errors, 0, 0, 0);
        }

        Map<String, Scheme> byName = new LinkedHashMap<>();
        int criteriaCount = 0;
        int documentCount = 0;
        int stepCount = 0;

        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            String type = cell(row, col, "type").toUpperCase(Locale.ROOT);
            if (type.isEmpty() || type.startsWith("#")) continue;                  // blank or comment row
            String where = "Row " + (r + 1) + ": ";
            String schemeName = cell(row, col, "scheme");
            if (schemeName.isEmpty()) {
                errors.add(where + "the scheme name is empty.");
                continue;
            }
            String key = schemeName.toLowerCase(Locale.ROOT);

            if (type.equals("SCHEME")) {
                if (byName.containsKey(key)) {
                    errors.add(where + "the scheme \"" + schemeName + "\" is defined twice in the file.");
                    continue;
                }
                Scheme s = new Scheme();
                s.setName(schemeName);
                s.setNameTa(blankToNull(cell(row, col, "name_ta")));
                s.setDescription(blankToNull(cell(row, col, "description")));
                s.setDescriptionTa(blankToNull(cell(row, col, "description_ta")));
                s.setBenefitType(blankToNull(cell(row, col, "benefit_type")));
                s.setCategory(blankToNull(cell(row, col, "category")));
                String amountText = cell(row, col, "benefit_amount");
                Double amount = amountText.isEmpty() ? Double.valueOf(0.0) : parseNumber(amountText);
                if (amount == null || amount.isNaN() || amount < 0) {
                    errors.add(where + "benefit_amount must be a number that is 0 or more.");
                    amount = 0.0;
                }
                s.setBenefitAmount(amount);
                checkLength(errors, where, "scheme name", s.getName(), 200);
                checkLength(errors, where, "name_ta", s.getNameTa(), 300);
                checkLength(errors, where, "description", s.getDescription(), 1000);
                checkLength(errors, where, "description_ta", s.getDescriptionTa(), 2000);
                checkLength(errors, where, "benefit_type", s.getBenefitType(), 50);
                checkLength(errors, where, "category", s.getCategory(), 100);
                byName.put(key, s);
                continue;
            }

            Scheme scheme = byName.get(key);
            if (scheme == null) {
                errors.add(where + "the scheme \"" + schemeName + "\" has not been defined by a SCHEME row above this row.");
                continue;
            }

            if (type.equals("CRITERION")) {
                EligibilityCriterion c = readCriterion(row, col, where, errors);
                if (c != null) {
                    c.setScheme(scheme);
                    scheme.getCriteria().add(c);
                    criteriaCount++;
                }
            } else if (type.equals("DOCUMENT")) {
                String docName = cell(row, col, "document_name");
                String reqText = cell(row, col, "requirement_type").toUpperCase(Locale.ROOT);
                RequirementType req = RequirementType.MANDATORY;
                if (!reqText.isEmpty()) {
                    try {
                        req = RequirementType.valueOf(reqText);
                    } catch (IllegalArgumentException e) {
                        errors.add(where + "requirement_type must be MANDATORY or OPTIONAL.");
                        continue;
                    }
                }
                if (docName.isEmpty() || docName.length() > 200) {
                    errors.add(where + "document_name is required and can have at most 200 characters.");
                    continue;
                }
                SchemeDocument d = new SchemeDocument();
                d.setScheme(scheme);
                d.setDocumentName(docName);
                d.setRequirementType(req);
                scheme.getDocuments().add(d);
                documentCount++;
            } else if (type.equals("STEP")) {
                String numberText = cell(row, col, "step_number");
                String stepText = cell(row, col, "step_text");
                String link = cell(row, col, "portal_link");
                int number;
                try {
                    number = Integer.parseInt(numberText);
                } catch (NumberFormatException e) {
                    errors.add(where + "step_number must be a whole number of 1 or more.");
                    continue;
                }
                boolean linkOk = link.isEmpty() || link.startsWith("https://") || link.startsWith("http://");
                if (number < 1 || stepText.isEmpty() || stepText.length() > 1000 || link.length() > 500 || !linkOk) {
                    errors.add(where + "a step needs a number of 1 or more, a description, and (optionally) an http(s) link.");
                    continue;
                }
                ApplicationStep st = new ApplicationStep();
                st.setScheme(scheme);
                st.setStepNumber(number);
                st.setDescription(stepText);
                st.setPortalLink(link.isEmpty() ? null : link);
                scheme.getSteps().add(st);
                stepCount++;
            } else {
                errors.add(where + "unknown type \"" + type + "\" (use SCHEME, CRITERION, DOCUMENT or STEP).");
            }
        }

        for (Scheme s : byName.values()) {
            if (s.getCriteria().isEmpty()) {
                errors.add("The scheme \"" + s.getName() + "\" has no CRITERION row. A scheme without rules would be shown as eligible to everyone.");
            }
        }
        return new ParsedFile(new ArrayList<>(byName.values()), errors, criteriaCount, documentCount, stepCount);
    }

    private EligibilityCriterion readCriterion(List<String> row, Map<String, Integer> col, String where, List<String> errors) {
        String parameter = cell(row, col, "parameter").toLowerCase(Locale.ROOT);
        if (!CitizenProfile.PARAMETERS.contains(parameter)) {
            errors.add(where + "parameter \"" + parameter + "\" is not a profile field. Use one of: "
                    + String.join(", ", CitizenProfile.PARAMETERS) + ".");
            return null;
        }
        CriterionOperator operator;
        try {
            operator = CriterionOperator.valueOf(cell(row, col, "operator").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.add(where + "operator must be one of BETWEEN, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL, EQUALS, IN_LIST.");
            return null;
        }
        Double min = parseNumber(cell(row, col, "min_value"));
        Double max = parseNumber(cell(row, col, "max_value"));
        if ((min != null && min.isNaN()) || (max != null && max.isNaN())) {
            errors.add(where + "min_value and max_value must be numbers.");
            return null;
        }
        String allowed = blankToNull(cell(row, col, "allowed_values"));
        boolean numericOperator = operator == CriterionOperator.BETWEEN
                || operator == CriterionOperator.LESS_THAN_OR_EQUAL
                || operator == CriterionOperator.GREATER_THAN_OR_EQUAL;
        if (numericOperator && !NUMERIC_PARAMETERS.contains(parameter)) {
            errors.add(where + operator + " can only be used with age, income or family_size; use EQUALS or IN_LIST for \"" + parameter + "\".");
            return null;
        }
        boolean valuesOk = switch (operator) {
            case BETWEEN -> min != null && max != null && min <= max;
            case LESS_THAN_OR_EQUAL -> max != null;
            case GREATER_THAN_OR_EQUAL -> min != null;
            default -> allowed != null;
        };
        if (!valuesOk) {
            errors.add(where + "fill in the values that " + operator + " needs (min_value, max_value or allowed_values).");
            return null;
        }
        if (allowed != null && allowed.length() > 255) {
            errors.add(where + "allowed_values can have at most 255 characters.");
            return null;
        }
        String mandatoryText = cell(row, col, "mandatory").toLowerCase(Locale.ROOT);
        boolean mandatory;
        if (mandatoryText.isEmpty() || mandatoryText.equals("true") || mandatoryText.equals("yes")
                || mandatoryText.equals("y") || mandatoryText.equals("1")) {
            mandatory = true;
        } else if (mandatoryText.equals("false") || mandatoryText.equals("no")
                || mandatoryText.equals("n") || mandatoryText.equals("0")) {
            mandatory = false;
        } else {
            errors.add(where + "mandatory must be true or false.");
            return null;
        }
        EligibilityCriterion c = new EligibilityCriterion();
        c.setParameter(parameter);
        c.setOperator(operator);
        c.setMinValue(min);
        c.setMaxValue(max);
        c.setAllowedValues(allowed);
        c.setMandatory(mandatory);
        return c;
    }

    // ---------- small helpers ----------

    private static String cell(List<String> row, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        if (i == null || i >= row.size()) return "";
        return row.get(i).trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Returns null for an empty cell, NaN for a cell that is not a number. */
    private static Double parseNumber(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            double d = Double.parseDouble(s.replace(",", "").trim());
            return Double.isFinite(d) ? Double.valueOf(d) : Double.valueOf(Double.NaN);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static void checkLength(List<String> errors, String where, String field, String value, int max) {
        if (value != null && value.length() > max) {
            errors.add(where + field + " can have at most " + max + " characters.");
        }
    }

    /** Minimal RFC 4180 reader: commas separate values, double quotes protect commas, quotes and line breaks. */
    static List<List<String>> readCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString());
                field.setLength(0);
                addRow(rows, row);
                row = new ArrayList<>();
            } else {
                field.append(ch);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("The file ends inside a quoted value (a closing quote is missing).");
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            addRow(rows, row);
        }
        return rows;
    }

    private static void addRow(List<List<String>> rows, List<String> row) {
        boolean blank = row.stream().allMatch(String::isBlank);
        if (!blank) rows.add(row);
    }
}
