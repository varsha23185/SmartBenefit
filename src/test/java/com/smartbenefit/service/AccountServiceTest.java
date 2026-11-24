package com.smartbenefit.service;

import com.smartbenefit.model.User;
import com.smartbenefit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Automated tests of "Change password" (test cases PW-01 to PW-06). */
class AccountServiceTest {

    private static final String EMAIL = "citizen@example.com";
    private static final String OLD = "Old12345";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserRepository repository;
    private AccountService service;
    private User user;
    private String originalHash;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(UserRepository.class);
        user = new User();
        user.setEmail(EMAIL);
        originalHash = encoder.encode(OLD);
        user.setPasswordHash(originalHash);
        Mockito.when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        service = new AccountService(repository, encoder);
    }

    @Test @DisplayName("PW-01 Correct old password and a valid new password: the new BCrypt hash is stored")
    void pw01() {
        String error = service.changePassword(EMAIL, OLD, "New12345", "New12345");

        assertThat(error).isNull();
        assertThat(user.getPasswordHash()).isNotEqualTo("New12345");            // never plain text
        assertThat(encoder.matches("New12345", user.getPasswordHash())).isTrue();
        assertThat(encoder.matches(OLD, user.getPasswordHash())).isFalse();
        verify(repository).save(user);
    }

    @Test @DisplayName("PW-02 Wrong old password is rejected and nothing is saved")
    void pw02() {
        String error = service.changePassword(EMAIL, "Wrong12345", "New12345", "New12345");

        assertThat(error).isEqualTo("error.oldpassword");
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        verify(repository, never()).save(any());
    }

    @Test @DisplayName("PW-03 A new password that is too short or has no letter or no digit is rejected")
    void pw03() {
        for (String weak : new String[] {"abc", "abcdefgh", "12345678", "Ab1"}) {
            assertThat(service.changePassword(EMAIL, OLD, weak, weak)).as(weak).isEqualTo("error.password");
        }
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        verify(repository, never()).save(any());
    }

    @Test @DisplayName("PW-04 A confirmation that does not match is rejected")
    void pw04() {
        String error = service.changePassword(EMAIL, OLD, "New12345", "New12346");

        assertThat(error).isEqualTo("error.confirm");
        verify(repository, never()).save(any());
    }

    @Test @DisplayName("PW-05 A new password equal to the old one is rejected")
    void pw05() {
        String error = service.changePassword(EMAIL, OLD, OLD, OLD);

        assertThat(error).isEqualTo("error.samepassword");
        verify(repository, never()).save(any());
    }

    @Test @DisplayName("PW-06 The password rule: 8 characters with a letter and a digit (boundary 7 and 8)")
    void pw06() {
        assertThat(PasswordPolicy.isValid("Abcdef12")).isTrue();     // exactly 8
        assertThat(PasswordPolicy.isValid("Abcde12")).isFalse();     // 7
        assertThat(PasswordPolicy.isValid(null)).isFalse();
    }
}
