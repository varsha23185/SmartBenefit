package com.smartbenefit.repository;

import com.smartbenefit.model.CitizenProfile;
import com.smartbenefit.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    @Query("select r from Recommendation r join fetch r.scheme where r.citizen = ?1 order by r.rankPosition")
    List<Recommendation> findByCitizenOrdered(CitizenProfile citizen);

    void deleteByCitizen(CitizenProfile citizen);

    /** Most frequently recommended schemes (for the admin analytics). */
    @Query("select r.scheme.name, count(r) from Recommendation r group by r.scheme.name order by count(r) desc")
    List<Object[]> topRecommendedSchemes();
}
