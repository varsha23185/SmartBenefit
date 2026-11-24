package com.smartbenefit.repository;

import com.smartbenefit.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findAllByOrderByDisplayOrderAscFaqIdAsc();
}
