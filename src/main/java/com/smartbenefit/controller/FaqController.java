package com.smartbenefit.controller;

import com.smartbenefit.repository.FaqRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The public FAQ page (also open to visitors who are not logged in). */
@Controller
public class FaqController {

    private final FaqRepository faqRepository;

    public FaqController(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("faqs", faqRepository.findAllByOrderByDisplayOrderAscFaqIdAsc());
        return "faq";
    }
}
