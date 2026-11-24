package com.smartbenefit.service;

import com.smartbenefit.model.Scheme;
import com.smartbenefit.repository.SchemeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Counts how often citizens open the application guidance page of a scheme ("schemes viewed" analytics). */
@Service
public class SchemeViewService {

    private final SchemeRepository schemeRepository;

    public SchemeViewService(SchemeRepository schemeRepository) {
        this.schemeRepository = schemeRepository;
    }

    /** Records one view. Views by the administrator (who only checks the page) are not counted. */
    public boolean recordView(Long schemeId, boolean viewerIsAdmin) {
        if (viewerIsAdmin) return false;
        schemeRepository.incrementViewCount(schemeId);
        return true;
    }

    public List<Scheme> mostViewed() {
        return schemeRepository.findTop5ByViewCountGreaterThanOrderByViewCountDescSchemeIdAsc(0L);
    }

    public long totalViews() {
        return schemeRepository.totalViews();
    }
}
