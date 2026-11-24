package com.smartbenefit.config;

import com.smartbenefit.model.*;
import com.smartbenefit.repository.FaqRepository;
import com.smartbenefit.repository.SchemeRepository;
import com.smartbenefit.repository.ScoringConfigRepository;
import com.smartbenefit.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs at start-up and loads demo data ONLY when the tables are empty:
 * an administrator account, the default ranking weights and five sample schemes.
 * NOTE: benefit amounts and thresholds are simplified for the prototype - check them against the official scheme rules.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    public static final String ADMIN_EMAIL = "admin@smartbenefit.com";
    public static final String ADMIN_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final SchemeRepository schemeRepository;
    private final ScoringConfigRepository scoringConfigRepository;
    private final FaqRepository faqRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, SchemeRepository schemeRepository,
                           ScoringConfigRepository scoringConfigRepository, FaqRepository faqRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.schemeRepository = schemeRepository;
        this.scoringConfigRepository = scoringConfigRepository;
        this.faqRepository = faqRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(ADMIN_EMAIL);
            u.setName("Administrator");
            u.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            u.setRole(UserRole.ADMIN);
            return userRepository.save(u);
        });

        if (scoringConfigRepository.count() == 0) {
            ScoringConfig c = new ScoringConfig();          // defaults 0.30 / 0.40 / 0.30
            c.setUpdatedBy(admin);
            scoringConfigRepository.save(c);
        }

        if (schemeRepository.count() == 0) seedSchemes();
        if (faqRepository.count() == 0) seedFaqs();
    }


    /** Starter questions; the administrator can edit, add, or delete them in the admin panel. */
    private void seedFaqs() {
        faqRepository.save(new Faq(
                "What is SmartBenefit?",
                "SmartBenefit என்றால் என்ன?",
                "SmartBenefit is a web application that shows which government welfare schemes you may be eligible for, explains why, ranks the eligible schemes, and guides you through the application.",
                "நீங்கள் எந்த அரசு நலத்திட்டங்களுக்குத் தகுதி பெறலாம் என்பதையும் அதற்கான காரணத்தையும் காட்டி, தகுதியான திட்டங்களை வரிசைப்படுத்தி, விண்ணப்பிக்கும் வழியையும் காட்டும் இணையப் பயன்பாடு இது.", 1));
        faqRepository.save(new Faq(
                "Does SmartBenefit apply for a scheme on my behalf?",
                "எனக்காக SmartBenefit திட்டத்திற்கு விண்ணப்பிக்குமா?",
                "No. SmartBenefit only advises and guides. You submit the application yourself on the official portal, and the government authority verifies and approves it.",
                "இல்லை. SmartBenefit ஆலோசனையும் வழிகாட்டுதலும் மட்டுமே தருகிறது. அதிகாரப்பூர்வ இணையதளத்தில் நீங்களே விண்ணப்பிக்க வேண்டும்; சரிபார்ப்பும் ஒப்புதலும் அரசு அதிகாரிகளுடையது.", 2));
        faqRepository.save(new Faq(
                "Why does a scheme show \"Additional Information Required\"?",
                "ஒரு திட்டத்திற்கு \"கூடுதல் தகவல் தேவை\" என்று ஏன் காட்டப்படுகிறது?",
                "Some details that the scheme needs are empty in your profile. Add them and check again. A missing detail is never counted as a failed condition.",
                "அந்தத் திட்டத்திற்குத் தேவையான சில விவரங்கள் உங்கள் சுயவிவரத்தில் காலியாக உள்ளன. அவற்றைச் சேர்த்து மீண்டும் சரிபார்க்கவும். விடுபட்ட விவரம் ஒருபோதும் தகுதியின்மையாகக் கருதப்படாது.", 3));
        faqRepository.save(new Faq(
                "How are the recommended schemes ranked?",
                "பரிந்துரைக்கப்பட்ட திட்டங்கள் எப்படி வரிசைப்படுத்தப்படுகின்றன?",
                "Each eligible scheme gets a score from its benefit amount, how well you match its conditions, and how many of your profile details it targets. The administrator can change the weights of these three parts.",
                "தகுதியான ஒவ்வொரு திட்டத்திற்கும் பயன் தொகை, நிபந்தனைகளுடன் உங்களின் பொருத்தம், திட்டம் இலக்காகக் கொண்ட உங்கள் சுயவிவர விவரங்கள் ஆகியவற்றின் அடிப்படையில் மதிப்பெண் வழங்கப்படுகிறது. இந்த மூன்று பகுதிகளின் எடைகளை நிர்வாகி மாற்றலாம்.", 4));
        faqRepository.save(new Faq(
                "What does the What-If Simulator do?",
                "என்ன-ஆனால் சோதனை என்ன செய்கிறது?",
                "It lets you change values such as income and see which schemes would change their status and why. Your saved profile is not changed.",
                "வருமானம் போன்ற மதிப்புகளை மாற்றினால் எந்தத் திட்டங்களின் நிலை ஏன் மாறும் என்பதைப் பார்க்கலாம். உங்கள் சேமிக்கப்பட்ட சுயவிவரம் மாறாது.", 5));
        faqRepository.save(new Faq(
                "Are the benefit amounts and rules official?",
                "தொகைகளும் விதிகளும் அதிகாரப்பூர்வமானவையா?",
                "The scheme names are real, but the amounts and limits in this prototype are simplified. Always check the official guidelines before you apply.",
                "திட்டங்களின் பெயர்கள் உண்மையானவை; ஆனால் இந்த மாதிரிப் பயன்பாட்டில் உள்ள தொகைகளும் வரம்புகளும் எளிமைப்படுத்தப்பட்டவை. விண்ணப்பிக்கும் முன் அதிகாரப்பூர்வ வழிகாட்டுதல்களைச் சரிபார்க்கவும்.", 6));
    }

    private void seedSchemes() {
        // 1. PM-KISAN
        Scheme kisan = scheme("PM-KISAN (Pradhan Mantri Kisan Samman Nidhi)",
                "பிரதமரின் கிசான் சம்மான் நிதி (PM-KISAN)",
                "Income support of Rs 6,000 per year for farmer families.",
                "விவசாயக் குடும்பங்களுக்கு ஆண்டுக்கு ரூ 6,000 வருமான உதவி.",
                "Cash", 6000, "Farmers");
        criterion(kisan, "occupation", CriterionOperator.EQUALS, null, null, "farmer", true);
        criterion(kisan, "income", CriterionOperator.LESS_THAN_OR_EQUAL, null, 1000000.0, null, true);
        document(kisan, "Aadhaar card", RequirementType.MANDATORY);
        document(kisan, "Land ownership records", RequirementType.MANDATORY);
        document(kisan, "Bank account passbook", RequirementType.MANDATORY);
        step(kisan, 1, "Open the PM-KISAN portal and choose 'New Farmer Registration'.", "https://pmkisan.gov.in");
        step(kisan, 2, "Enter Aadhaar, mobile number and state, then fill in land and bank details.", "https://pmkisan.gov.in");
        step(kisan, 3, "Submit and note the registration number; the state authority verifies the details.", null);
        schemeRepository.save(kisan);

        // 2. PMAY-G
        Scheme pmayg = scheme("PMAY-G (Pradhan Mantri Awas Yojana - Gramin)",
                "பிரதமரின் ஆவாஸ் யோஜனா - கிராமின் (PMAY-G)",
                "Financial assistance for building a pucca house in rural areas.",
                "கிராமப்புறங்களில் உறுதியான வீடு கட்டுவதற்கான நிதி உதவி.",
                "Housing", 120000, "Rural households");
        criterion(pmayg, "location", CriterionOperator.EQUALS, null, null, "rural", true);
        criterion(pmayg, "income", CriterionOperator.LESS_THAN_OR_EQUAL, null, 300000.0, null, true);
        criterion(pmayg, "category", CriterionOperator.IN_LIST, null, null, "SC,ST,OBC", false);
        document(pmayg, "Aadhaar card", RequirementType.MANDATORY);
        document(pmayg, "Job card (MGNREGA)", RequirementType.OPTIONAL);
        document(pmayg, "Bank account passbook", RequirementType.MANDATORY);
        step(pmayg, 1, "Contact the Gram Panchayat; beneficiaries are identified from the SECC / Awaas+ list.", "https://pmayg.nic.in");
        step(pmayg, 2, "Complete the verification and geo-tagging of the existing house.", null);
        step(pmayg, 3, "After approval, the assistance is paid in instalments to the bank account.", null);
        schemeRepository.save(pmayg);

        // 3. Sukanya Samriddhi (the profile is the girl child's, entered by the guardian)
        Scheme sukanya = scheme("Sukanya Samriddhi Yojana",
                "சுகன்யா சம்ருத்தி யோஜனா",
                "Small-savings account for the girl child (profile of the girl child, entered by the guardian).",
                "பெண் குழந்தைக்கான சிறு சேமிப்புக் கணக்கு (பாதுகாவலர் குழந்தையின் விவரங்களை உள்ளிடவேண்டும்).",
                "Savings", 50000, "Girl child");
        criterion(sukanya, "age", CriterionOperator.BETWEEN, 0.0, 10.0, null, true);
        document(sukanya, "Birth certificate of the girl child", RequirementType.MANDATORY);
        document(sukanya, "Identity and address proof of the guardian", RequirementType.MANDATORY);
        step(sukanya, 1, "Visit a post office or authorised bank with the documents.", "https://www.indiapost.gov.in");
        step(sukanya, 2, "Fill in the account opening form and make the initial deposit.", null);
        schemeRepository.save(sukanya);

        // 4. Widow pension
        Scheme widow = scheme("Widow Pension Scheme",
                "விதவை ஓய்வூதியத் திட்டம்",
                "Monthly pension for widows who meet the age and income conditions.",
                "வயது மற்றும் வருமான நிபந்தனைகளைப் பூர்த்தி செய்யும் விதவைகளுக்கான மாத ஓய்வூதியம்.",
                "Cash", 12000, "Widows");
        criterion(widow, "marital_status", CriterionOperator.EQUALS, null, null, "widowed", true);
        criterion(widow, "age", CriterionOperator.GREATER_THAN_OR_EQUAL, 40.0, null, null, true);
        criterion(widow, "income", CriterionOperator.LESS_THAN_OR_EQUAL, null, 100000.0, null, true);
        document(widow, "Husband's death certificate", RequirementType.MANDATORY);
        document(widow, "Aadhaar card", RequirementType.MANDATORY);
        document(widow, "Income certificate", RequirementType.MANDATORY);
        step(widow, 1, "Apply at the taluk office / e-Sevai centre or through the state social welfare portal.", "https://www.myscheme.gov.in");
        step(widow, 2, "Submit the documents; the revenue officer verifies the application.", null);
        schemeRepository.save(widow);

        // 5. MGNREGA
        Scheme nrega = scheme("MGNREGA (Mahatma Gandhi National Rural Employment Guarantee Act)",
                "மகாத்மா காந்தி தேசிய ஊரக வேலை உறுதித் திட்டம் (MGNREGA)",
                "Guaranteed wage employment for rural households (up to 100 days a year).",
                "ஊரக குடும்பங்களுக்கு ஆண்டுக்கு 100 நாட்கள் வரை உறுதியான கூலி வேலை.",
                "Employment", 30000, "Rural households");
        criterion(nrega, "location", CriterionOperator.EQUALS, null, null, "rural", true);
        criterion(nrega, "age", CriterionOperator.GREATER_THAN_OR_EQUAL, 18.0, null, null, true);
        document(nrega, "Passport-size photograph", RequirementType.MANDATORY);
        document(nrega, "Aadhaar card", RequirementType.OPTIONAL);
        step(nrega, 1, "Apply to the Gram Panchayat for a Job Card.", "https://nrega.nic.in");
        step(nrega, 2, "After the job card is issued, submit a written application for work.", null);
        schemeRepository.save(nrega);
    }

    private Scheme scheme(String name, String nameTa, String desc, String descTa, String type, double amount, String category) {
        Scheme s = new Scheme();
        s.setName(name); s.setNameTa(nameTa);
        s.setDescription(desc); s.setDescriptionTa(descTa);
        s.setBenefitType(type); s.setBenefitAmount(amount); s.setCategory(category);
        return s;
    }

    private void criterion(Scheme s, String parameter, CriterionOperator op, Double min, Double max, String allowed, boolean mandatory) {
        EligibilityCriterion c = new EligibilityCriterion();
        c.setScheme(s); c.setParameter(parameter); c.setOperator(op);
        c.setMinValue(min); c.setMaxValue(max); c.setAllowedValues(allowed); c.setMandatory(mandatory);
        s.getCriteria().add(c);
    }

    private void document(Scheme s, String name, RequirementType type) {
        SchemeDocument d = new SchemeDocument();
        d.setScheme(s); d.setDocumentName(name); d.setRequirementType(type);
        s.getDocuments().add(d);
    }

    private void step(Scheme s, int number, String description, String link) {
        ApplicationStep st = new ApplicationStep();
        st.setScheme(s); st.setStepNumber(number); st.setDescription(description); st.setPortalLink(link);
        s.getSteps().add(st);
    }
}
