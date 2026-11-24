package com.smartbenefit.repository;

import com.smartbenefit.model.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SchemeRepository extends JpaRepository<Scheme, Long> {

    /** Used by the CSV import to skip schemes that already exist (case-insensitive). */
    boolean existsByNameIgnoreCase(String name);

    /** Loads every scheme together with its criteria in a single query. */
    @Query("select distinct s from Scheme s left join fetch s.criteria order by s.schemeId")
    List<Scheme> findAllWithCriteria();

    @Query("select s from Scheme s left join fetch s.criteria where s.schemeId = :id")
    Optional<Scheme> findByIdWithCriteria(@Param("id") Long id);

    /** Adds one to the view counter in a single atomic update (no read-modify-write race). */
    @Modifying
    @Transactional
    @Query("update Scheme s set s.viewCount = s.viewCount + 1 where s.schemeId = :id")
    int incrementViewCount(@Param("id") Long id);

    /** The five most viewed schemes that have been viewed at least once. */
    List<Scheme> findTop5ByViewCountGreaterThanOrderByViewCountDescSchemeIdAsc(long minimum);

    @Query("select coalesce(sum(s.viewCount), 0L) from Scheme s")
    long totalViews();
}
