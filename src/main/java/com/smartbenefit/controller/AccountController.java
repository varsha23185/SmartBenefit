package com.smartbenefit.controller;

import com.smartbenefit.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** "Change password" for every logged-in user (citizen or administrator). */
@Controller
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/account/password")
    public String form() { return "change-password"; }

    @PostMapping("/account/password")
    public String change(@RequestParam String oldPassword, @RequestParam String newPassword,
                         @RequestParam String confirmPassword, Principal principal,
                         Model model, RedirectAttributes ra) {
        String error = accountService.changePassword(principal.getName(), oldPassword, newPassword, confirmPassword);
        if (error != null) {
            model.addAttribute("error", error);
            return "change-password";
        }
        ra.addFlashAttribute("changed", true);
        return "redirect:/account/password";
    }
}
