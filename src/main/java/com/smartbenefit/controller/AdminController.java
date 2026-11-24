package com.smartbenefit.controller;

import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.*;
import com.smartbenefit.repository.*;
import com.smartbenefit.service.SchemeViewService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final List<String> PARAMETERS = CitizenProfile.PARAMETERS;

    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final EligibilityAssessmentRepository assessmentRepository;
    private final RecommendationRepository recommendationRepository;
    private final ScoringConfigRepository scoringConfigRepository;
    private final SchemeViewService schemeViewService;

    public AdminController(SchemeRepository schemeRepository, UserRepository userRepository,
                           EligibilityAssessmentRepository assessmentRepository,
                           RecommendationRepository recommendationRepository,
                           ScoringConfigRepository scoringConfigRepository,
                           SchemeViewService schemeViewService) {
        this.schemeRepository = schemeRepository;
        this.userRepository = userRepository;
        this.assessmentRepository = assessmentRepository;
        this.recommendationRepository = recommendationRepository;
        this.scoringConfigRepository = scoringConfigRepository;
        this.schemeViewService = schemeViewService;
    }

    // ---------- dashboard + analytics + weights ----------
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("schemes", schemeRepository.findAll());
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("assessmentCount", assessmentRepository.count());
        model.addAttribute("topSchemes", recommendationRepository.topRecommendedSchemes());
        model.addAttribute("viewedSchemes", schemeViewService.mostViewed());
        model.addAttribute("totalViews", schemeViewService.totalViews());
        model.addAttribute("config", scoringConfigRepository.findTopByOrderByConfigIdDesc().orElseGet(ScoringConfig::new));
        return "admin";
    }

    @PostMapping("/weights")
    public String saveWeights(@RequestParam double w1, @RequestParam double w2, @RequestParam double w3,
                              Principal principal, RedirectAttributes ra) {
        ScoringConfig c = new ScoringConfig();
        c.setW1Benefit(w1); c.setW2Match(w2); c.setW3Relevance(w3);
        if (!c.isValid()) {
            ra.addFlashAttribute("error", "The three weights must be 0 or more and add up to 1.");
            return "redirect:/admin";
        }
        c.setUpdatedBy(userRepository.findByEmail(principal.getName()).orElse(null));
        c.setUpdatedAt(LocalDateTime.now());
        scoringConfigRepository.save(c);       // a new row; the newest row is the active one
        ra.addFlashAttribute("message", "Ranking weights updated.");
        return "redirect:/admin";
    }

    // ---------- schemes ----------
    @GetMapping("/scheme/new")
    public String newScheme(Model model) {
        model.addAttribute("scheme", new Scheme());
        return "admin-scheme";
    }

    @GetMapping("/scheme/{id}")
    public String editScheme(@PathVariable Long id, Model model) {
        model.addAttribute("scheme", schemeRepository.findById(id).orElseThrow());
        model.addAttribute("parameters", PARAMETERS);
        model.addAttribute("operators", CriterionOperator.values());
        return "admin-scheme";
    }

    @PostMapping("/scheme")
    @Transactional
    public String saveScheme(@RequestParam(required = false) Long schemeId, @RequestParam String name,
                             @RequestParam(required = false) String nameTa, @RequestParam(required = false) String description,
                             @RequestParam(required = false) String descriptionTa, @RequestParam(required = false) String benefitType,
                             @RequestParam(required = false) Double benefitAmount, @RequestParam(required = false) String category,
                             RedirectAttributes ra) {
        if (name.isBlank() || (benefitAmount != null && benefitAmount < 0)) {
            ra.addFlashAttribute("error", "Scheme name is required and the benefit amount cannot be negative.");
            return schemeId == null ? "redirect:/admin/scheme/new" : "redirect:/admin/scheme/" + schemeId;
        }
        Scheme s = schemeId == null ? new Scheme() : schemeRepository.findById(schemeId).orElseThrow();
        s.setName(name.trim()); s.setNameTa(nameTa); s.setDescription(description); s.setDescriptionTa(descriptionTa);
        s.setBenefitType(benefitType); s.setBenefitAmount(benefitAmount == null ? 0.0 : benefitAmount); s.setCategory(category);
        s = schemeRepository.save(s);
        ra.addFlashAttribute("message", "Scheme saved.");
        return "redirect:/admin/scheme/" + s.getSchemeId();
    }

    @PostMapping("/scheme/{id}/delete")
    public String deleteScheme(@PathVariable Long id, RedirectAttributes ra) {
        try {
            schemeRepository.deleteById(id);
            schemeRepository.flush();
            ra.addFlashAttribute("message", "Scheme deleted.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "This scheme already has assessments, so it cannot be deleted.");
        }
        return "redirect:/admin";
    }

    // ---------- criteria ----------
    @PostMapping("/scheme/{id}/criterion")
    @Transactional
    public String addCriterion(@PathVariable Long id, @RequestParam String parameter, @RequestParam CriterionOperator operator,
                               @RequestParam(required = false) Double minValue, @RequestParam(required = false) Double maxValue,
                               @RequestParam(required = false) String allowedValues,
                               @RequestParam(defaultValue = "false") boolean mandatory, RedirectAttributes ra) {
        boolean ok = PARAMETERS.contains(parameter) && (switch (operator) {
            case BETWEEN -> minValue != null && maxValue != null && minValue <= maxValue;
            case LESS_THAN_OR_EQUAL -> maxValue != null;
            case GREATER_THAN_OR_EQUAL -> minValue != null;
            default -> allowedValues != null && !allowedValues.isBlank();
        });
        if (!ok) {
            ra.addFlashAttribute("error", "Invalid criterion: fill in the values needed by the chosen operator.");
            return "redirect:/admin/scheme/" + id;
        }
        Scheme s = schemeRepository.findById(id).orElseThrow();
        EligibilityCriterion c = new EligibilityCriterion();
        c.setScheme(s); c.setParameter(parameter); c.setOperator(operator);
        c.setMinValue(minValue); c.setMaxValue(maxValue); c.setAllowedValues(allowedValues); c.setMandatory(mandatory);
        s.getCriteria().add(c);
        schemeRepository.save(s);
        ra.addFlashAttribute("message", "Criterion added.");
        return "redirect:/admin/scheme/" + id;
    }

    @PostMapping("/scheme/{id}/criterion/{cid}/delete")
    @Transactional
    public String deleteCriterion(@PathVariable Long id, @PathVariable Long cid) {
        Scheme s = schemeRepository.findById(id).orElseThrow();
        s.getCriteria().removeIf(c -> c.getCriterionId().equals(cid));
        return "redirect:/admin/scheme/" + id;
    }

    // ---------- documents and steps ----------
    @PostMapping("/scheme/{id}/document")
    @Transactional
    public String addDocument(@PathVariable Long id, @RequestParam String documentName,
                              @RequestParam RequirementType requirementType) {
        if (!documentName.isBlank()) {
            Scheme s = schemeRepository.findById(id).orElseThrow();
            SchemeDocument d = new SchemeDocument();
            d.setScheme(s); d.setDocumentName(documentName.trim()); d.setRequirementType(requirementType);
            s.getDocuments().add(d);
        }
        return "redirect:/admin/scheme/" + id;
    }

    @PostMapping("/scheme/{id}/document/{did}/delete")
    @Transactional
    public String deleteDocument(@PathVariable Long id, @PathVariable Long did) {
        schemeRepository.findById(id).orElseThrow().getDocuments().removeIf(d -> d.getDocumentId().equals(did));
        return "redirect:/admin/scheme/" + id;
    }

    @PostMapping("/scheme/{id}/step")
    @Transactional
    public String addStep(@PathVariable Long id, @RequestParam Integer stepNumber, @RequestParam String description,
                          @RequestParam(required = false) String portalLink, RedirectAttributes ra) {
        boolean linkOk = portalLink == null || portalLink.isBlank() || portalLink.startsWith("https://") || portalLink.startsWith("http://");
        if (description.isBlank() || stepNumber == null || stepNumber < 1 || !linkOk) {
            ra.addFlashAttribute("error", "Invalid step: a number, a description, and (optionally) an http(s) link are needed.");
            return "redirect:/admin/scheme/" + id;
        }
        Scheme s = schemeRepository.findById(id).orElseThrow();
        ApplicationStep st = new ApplicationStep();
        st.setScheme(s); st.setStepNumber(stepNumber); st.setDescription(description.trim());
        st.setPortalLink(portalLink == null || portalLink.isBlank() ? null : portalLink.trim());
        s.getSteps().add(st);
        return "redirect:/admin/scheme/" + id;
    }

    @PostMapping("/scheme/{id}/step/{sid}/delete")
    @Transactional
    public String deleteStep(@PathVariable Long id, @PathVariable Long sid) {
        schemeRepository.findById(id).orElseThrow().getSteps().removeIf(st -> st.getStepId().equals(sid));
        return "redirect:/admin/scheme/" + id;
    }
}
