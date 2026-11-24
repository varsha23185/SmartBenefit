package com.smartbenefit.service;

import com.smartbenefit.model.User;
import com.smartbenefit.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account functions of a logged-in user. */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Changes the password of the user with the given e-mail.
     *
     * @return null when the password was changed, otherwise the message key of the first problem found
     */
    @Transactional
    public String changePassword(String email, String oldPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findByEmail(email).orElseThrow();
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) return "error.oldpassword";
        if (!PasswordPolicy.isValid(newPassword)) return "error.password";
        if (!newPassword.equals(confirmPassword)) return "error.confirm";
        if (newPassword.equals(oldPassword)) return "error.samepassword";
        user.setPasswordHash(passwordEncoder.encode(newPassword));     // only the BCrypt hash is stored
        userRepository.save(user);
        return null;
    }
}
