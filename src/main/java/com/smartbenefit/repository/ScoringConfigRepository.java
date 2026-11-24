package com.smartbenefit.repository;

import com.smartbenefit.model.ScoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScoringConfigRepository extends JpaRepository<ScoringConfig, Long> {
    Optional<ScoringConfig> findTopByOrderByConfigIdDesc();
}
