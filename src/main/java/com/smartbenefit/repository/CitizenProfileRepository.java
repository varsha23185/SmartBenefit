package com.smartbenefit.repository;

import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, Long> {
    Optional<CitizenProfile> findByUser(User user);
}
