package com.smartbenefit.service;

import com.smartbenefit.model.Faq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Automated tests of the FAQ language fallback (test cases FQ-01 and FQ-02). */
class FaqTest {

    @Test @DisplayName("FQ-01 In Tamil, the Tamil text is shown when it exists")
    void fq01() {
        Faq faq = new Faq("Question", "Question-ta", "Answer", "Answer-ta", 1);

        assertThat(faq.localQuestion("ta")).isEqualTo("Question-ta");
        assertThat(faq.localAnswer("ta")).isEqualTo("Answer-ta");
        assertThat(faq.localQuestion("en")).isEqualTo("Question");
        assertThat(faq.localAnswer("en")).isEqualTo("Answer");
    }

    @Test @DisplayName("FQ-02 In Tamil, an empty Tamil value falls back to the English text")
    void fq02() {
        Faq faq = new Faq("Question", "  ", "Answer", null, 1);

        assertThat(faq.localQuestion("ta")).isEqualTo("Question");
        assertThat(faq.localAnswer("ta")).isEqualTo("Answer");
    }
}
