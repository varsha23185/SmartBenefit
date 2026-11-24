package com.smartbenefit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One frequently asked question, stored in English and (optionally) Tamil. Edited by the administrator. */
@Entity
@Table(name = "faq")
@Getter @Setter @NoArgsConstructor
public class Faq {
    public static final int MAX_QUESTION = 300;
    public static final int MAX_ANSWER = 2000;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long faqId;

    @Column(nullable = false, length = 300) private String question;
    @Column(name = "question_ta", length = 600) private String questionTa;
    @Column(nullable = false, length = 2000) private String answer;
    @Column(name = "answer_ta", length = 4000) private String answerTa;

    /** Smaller numbers are shown first. */
    @Column(name = "display_order", nullable = false) private int displayOrder = 0;

    public Faq(String question, String questionTa, String answer, String answerTa, int displayOrder) {
        this.question = question;
        this.questionTa = questionTa;
        this.answer = answer;
        this.answerTa = answerTa;
        this.displayOrder = displayOrder;
    }

    /** Tamil question when the interface language is Tamil and a Tamil value exists; English otherwise. */
    public String localQuestion(String lang) {
        return "ta".equals(lang) && questionTa != null && !questionTa.isBlank() ? questionTa : question;
    }

    public String localAnswer(String lang) {
        return "ta".equals(lang) && answerTa != null && !answerTa.isBlank() ? answerTa : answer;
    }
}
