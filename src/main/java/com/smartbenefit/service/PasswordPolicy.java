package com.smartbenefit.service;

/** The one password rule used by registration and by "Change password": at least 8 characters, a letter and a digit. */
public final class PasswordPolicy {

    private PasswordPolicy() { }

    public static boolean isValid(String password) {
        return password != null
                && password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*");
    }
}
