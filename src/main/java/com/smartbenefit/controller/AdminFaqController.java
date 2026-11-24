package com.smartbenefit.controller;

import com.smartbenefit.model.Faq;
import com.smartbenefit.repository.FaqRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Administrator screen to add, edit, and delete FAQ entries (protected by the /admin/** rule). */
@Controller
@RequestMapping("/admin/faq")
public class AdminFaqController {

    private final FaqRepository faqRepository;

    public AdminFaqController(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("faqs", faqRepository.findAllByOrderByDisplayOrderAscFaqIdAsc());
        model.addAttribute("faq", new Faq());
        return "admin-faq";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("faqs", faqRepository.findAllByOrderByDisplayOrderAscFaqIdAsc());
        model.addAttribute("faq", faqRepository.findById(id).orElseThrow());
        return "admin-faq";
    }

    @PostMapping
    public String save(@RequestParam(required = false) Long faqId, @RequestParam String question,
                       @RequestParam(required = false) String questionTa, @RequestParam String answer,
                       @RequestParam(required = false) String answerTa,
                       @RequestParam(defaultValue = "0") int displayOrder, RedirectAttributes ra) {
        String back = faqId == null ? "redirect:/admin/faq" : "redirect:/admin/faq/" + faqId;
        if (question.isBlank() || answer.isBlank()) {
            ra.addFlashAttribute("error", "The question and the answer (in English) are required.");
            return back;
        }
        if (question.length() > Faq.MAX_QUESTION || answer.length() > Faq.MAX_ANSWER) {
            ra.addFlashAttribute("error", "The question can have at most " + Faq.MAX_QUESTION
                    + " characters and the answer at most " + Faq.MAX_ANSWER + " characters.");
            return back;
        }
        Faq f = faqId == null ? new Faq() : faqRepository.findById(faqId).orElseThrow();
        f.setQuestion(question.trim());
        f.setQuestionTa(blankToNull(questionTa));
        f.setAnswer(answer.trim());
        f.setAnswerTa(blankToNull(answerTa));
        f.setDisplayOrder(displayOrder);
        faqRepository.save(f);
        ra.addFlashAttribute("message", faqId == null ? "FAQ added." : "FAQ updated.");
        return "redirect:/admin/faq";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        faqRepository.deleteById(id);
        ra.addFlashAttribute("message", "FAQ deleted.");
        return "redirect:/admin/faq";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
