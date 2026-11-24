package com.smartbenefit.repository;

import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.EligibilityAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EligibilityAssessmentRepository extends JpaRepository<EligibilityAssessment, Long> {

    /** The most recent assessment of the citizen for each scheme. */
    @Query("select a from EligibilityAssessment a join fetch a.scheme where a.citizen = :c " +
           "and a.assessedAt = (select max(b.assessedAt) from EligibilityAssessment b " +
           "where b.citizen = :c and b.scheme = a.scheme) order by a.scheme.schemeId")
    List<EligibilityAssessment> findLatestByCitizen(@Param("c") CitizenProfile citizen);
}
