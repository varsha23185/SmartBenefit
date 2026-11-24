package com.smartbenefit.dto;

import com.smartbenefit.model.CitizenProfile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/** Form object used by the profile page and by the what-if simulator. */
@Getter @Setter
public class ProfileForm {
    @Min(value = 0, message = "Age must be between 0 and 120")
    @Max(value = 120, message = "Age must be between 0 and 120")
    private Integer age;

    @PositiveOrZero(message = "Income cannot be negative")
    private Double income;

    @Min(value = 1, message = "Family size must be between 1 and 50")
    @Max(value = 50, message = "Family size must be between 1 and 50")
    private Integer familySize;

    private String educationLevel;
    private String location;
    private String occupation;
    private String category;
    private String maritalStatus;
    private String gender;

    public static ProfileForm from(CitizenProfile p) {
        ProfileForm f = new ProfileForm();
        f.age = p.getAge();
        f.income = p.getIncome();
        f.familySize = p.getFamilySize();
        f.educationLevel = p.getEducationLevel();
        f.location = p.getLocation();
        f.occupation = p.getOccupation();
        f.category = p.getCategory();
        f.maritalStatus = p.getMaritalStatus();
        f.gender = p.getGender();
        return f;
    }

    /** Overwrites every field of the profile (blank text becomes null = "not provided"). */
    public void applyTo(CitizenProfile p) {
        p.setAge(age);
        p.setIncome(income);
        p.setFamilySize(familySize);
        p.setEducationLevel(blankToNull(educationLevel));
        p.setLocation(blankToNull(location));
        p.setOccupation(blankToNull(occupation));
        p.setCategory(blankToNull(category));
        p.setMaritalStatus(blankToNull(maritalStatus));
        p.setGender(blankToNull(gender));
    }

    /** Changes only the fields the citizen filled in (used for what-if scenarios on a COPY of the profile). */
    public void overlayOn(CitizenProfile p) {
        if (age != null) p.setAge(age);
        if (income != null) p.setIncome(income);
        if (familySize != null) p.setFamilySize(familySize);
        if (!isBlank(educationLevel)) p.setEducationLevel(educationLevel);
        if (!isBlank(location)) p.setLocation(location);
        if (!isBlank(occupation)) p.setOccupation(occupation);
        if (!isBlank(category)) p.setCategory(category);
        if (!isBlank(maritalStatus)) p.setMaritalStatus(maritalStatus);
        if (!isBlank(gender)) p.setGender(gender);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String blankToNull(String s) { return isBlank(s) ? null : s.trim(); }
}
