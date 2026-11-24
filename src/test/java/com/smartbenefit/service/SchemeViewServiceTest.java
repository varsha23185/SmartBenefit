package com.smartbenefit.service;

import com.smartbenefit.model.Scheme;
import com.smartbenefit.repository.SchemeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Automated tests of the "schemes viewed" counter (test cases SV-01 to SV-03). */
class SchemeViewServiceTest {

    private final SchemeRepository repository = Mockito.mock(SchemeRepository.class);
    private final SchemeViewService service = new SchemeViewService(repository);

    @Test @DisplayName("SV-01 A citizen opening a scheme adds one view")
    void sv01() {
        boolean counted = service.recordView(5L, false);

        assertThat(counted).isTrue();
        verify(repository).incrementViewCount(5L);
    }

    @Test @DisplayName("SV-02 A view by the administrator is not counted")
    void sv02() {
        boolean counted = service.recordView(5L, true);

        assertThat(counted).isFalse();
        verify(repository, never()).incrementViewCount(anyLong());
    }

    @Test @DisplayName("SV-03 The most viewed schemes come from the repository, highest first")
    void sv03() {
        Scheme first = new Scheme();
        first.setName("First");
        Scheme second = new Scheme();
        second.setName("Second");
        Mockito.when(repository.findTop5ByViewCountGreaterThanOrderByViewCountDescSchemeIdAsc(0L))
                .thenReturn(List.of(first, second));

        assertThat(service.mostViewed()).containsExactly(first, second);
    }
}
