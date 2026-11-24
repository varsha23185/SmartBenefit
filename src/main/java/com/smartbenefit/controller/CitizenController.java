package com.smartbenefit.controller;

import com.smartbenefit.dto.ProfileForm;
import com.smartbenefit.model.*;
import com.smartbenefit.repository.*;
import com.smartbenefit.service.EligibilityCheckService;
import com.smartbenefit.service.SchemeViewService;
import com.smartbenefit.service.WhatIfService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class CitizenController {

    private final UserRepository userRepository;
    private final CitizenProfileRepository profileRepository;
    private final SchemeRepository schemeRepository;
    private final EligibilityAssessmentRepository assessmentRepository;
    private final RecommendationRepository recommendationRepository;
    private final EligibilityCheckService checkService;
    private final WhatIfService whatIfService;
    private final SchemeViewService schemeViewService;

    public CitizenController(UserRepository userRepository, CitizenProfileRepository profileRepository,
                             SchemeRepository schemeRepository, EligibilityAssessmentRepository assessmentRepository,
                             RecommendationRepository recommendationRepository,
                             EligibilityCheckService checkService, WhatIfService whatIfService,
                             SchemeViewService schemeViewService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.schemeRepository = schemeRepository;
        this.assessmentRepository = assessmentRepository;
        this.recommendationRepository = recommendationRepository;
        this.checkService = checkService;
        this.whatIfService = whatIfService;
        this.schemeViewService = schemeViewService;
    }

    /** A citizen only ever reaches his or her own profile: it is looked up from the logged-in e-mail. */
    private CitizenProfile profileOf(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return profileRepository.findByUser(user).orElseGet(() -> {
            CitizenProfile p = new CitizenProfile();
            p.setUser(user);
            return profileRepository.save(p);
        });
    }

    /** Visitors see the home page; a logged-in user goes straight to the dashboard. */
    @GetMapping("/")
    public String home(java.security.Principal principal) {
        return principal != null ? "redirect:/dashboard" : "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Principal principal, Model model) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin";
        }
        CitizenProfile profile = profileOf(principal);
        model.addAttribute("userName", profile.getUser().getName());
        model.addAttribute("recommendations", recommendationRepository.findByCitizenOrdered(profile));
        return "dashboard";
    }

    // ---------- profile ----------
    @GetMapping("/profile")
    public String profileForm(Principal principal, Model model) {
        model.addAttribute("form", ProfileForm.from(profileOf(principal)));
        return "profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@Valid @ModelAttribute("form") ProfileForm form, BindingResult result,
                              Principal principal, RedirectAttributes ra) {
        if (result.hasErrors()) return "profile";
        CitizenProfile profile = profileOf(principal);
        form.applyTo(profile);
        profileRepository.save(profile);
        ra.addFlashAttribute("saved", true);
        return "redirect:/profile";
    }

    // ---------- eligibility ----------
    @PostMapping("/check")
    public String check(Principal principal) {
        checkService.check(profileOf(principal));
        return "redirect:/results";
    }

    @GetMapping("/results")
    public String results(Principal principal, Model model) {
        CitizenProfile profile = profileOf(principal);
        model.addAttribute("assessments", assessmentRepository.findLatestByCitizen(profile));
        model.addAttribute("recommendations", recommendationRepository.findByCitizenOrdered(profile));
        return "results";
    }

    // ---------- application guidance ----------
    @GetMapping("/scheme/{id}")
    public String guidance(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("scheme", schemeRepository.findById(id).orElseThrow());
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        schemeViewService.recordView(id, admin);       // "schemes viewed" analytics (citizen views only)
        return "scheme";
    }

    // ---------- what-if ----------
    @GetMapping("/whatif")
    public String whatIfForm(Principal principal, Model model) {
        model.addAttribute("form", new ProfileForm());
        model.addAttribute("current", ProfileForm.from(profileOf(principal)));
        return "whatif";
    }

    @PostMapping("/whatif")
    public String whatIf(@Valid @ModelAttribute("form") ProfileForm form, BindingResult result,
                         Principal principal, Model model) {
        CitizenProfile profile = profileOf(principal);
        model.addAttribute("current", ProfileForm.from(profile));
        if (!result.hasErrors()) {
            model.addAttribute("changes", whatIfService.simulate(profile, form));
        }
        return "whatif";
    }
}
