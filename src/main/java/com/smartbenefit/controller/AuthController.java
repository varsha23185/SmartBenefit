package com.smartbenefit.controller;

import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.User;
import com.smartbenefit.model.UserRole;
import com.smartbenefit.repository.CitizenProfileRepository;
import com.smartbenefit.repository.UserRepository;
import com.smartbenefit.service.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

@Controller
public class AuthController {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final CitizenProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, CitizenProfileRepository profileRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    @PostMapping("/register")
    @Transactional
    public String register(@RequestParam String name, @RequestParam String email,
                           @RequestParam String password, Model model) {
        String cleanEmail = email.trim().toLowerCase();
        String error = null;
        if (name.isBlank()) error = "error.name";
        else if (!EMAIL.matcher(cleanEmail).matches()) error = "error.email";
        else if (!PasswordPolicy.isValid(password)) error = "error.password";
        else if (userRepository.existsByEmail(cleanEmail)) error = "error.duplicate";

        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(cleanEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.CITIZEN);
        userRepository.save(user);

        CitizenProfile profile = new CitizenProfile();     // empty profile, filled in later by the citizen
        profile.setUser(user);
        profileRepository.save(profile);
        return "redirect:/login?registered";
    }
}
