# SmartBenefit - Spring Boot project (MCA Review II)

Spring Boot 3.3 | Java 17 | MySQL 8 | Spring Data JPA | Spring Security | Thymeleaf (Tamil / English)

## 1. Install (one time)
| Software | Version | Notes |
|---|---|---|
| JDK | 17 or newer (21 is fine) | Eclipse Temurin: https://adoptium.net . Check with `java -version` |
| Maven | 3.9+ | https://maven.apache.org/download.cgi (or use the Maven built into IntelliJ / STS) |
| MySQL Server + Workbench | 8.x | https://dev.mysql.com/downloads/installer/ . Remember the **root password** you set |
| IDE | IntelliJ IDEA Community or Spring Tool Suite | Lombok: IntelliJ works out of the box (enable *Annotation Processing* if asked). STS/Eclipse: run the Lombok installer once (https://projectlombok.org/setup/eclipse) |

## 2. Configure the database password
Open `src/main/resources/application.properties` and replace `YOUR_MYSQL_PASSWORD` with your MySQL root password.

Alternatively, leave the file unchanged and set an environment variable before starting the app, so no password is stored in the code:

* Windows (Command Prompt): `set DB_PASSWORD=your_password` then `mvn spring-boot:run`
* Linux / macOS: `export DB_PASSWORD=your_password` then `mvn spring-boot:run`
You do NOT need to create the database or the tables - the application creates the `smartbenefit` database, all nine tables and the demo data on first start.

## 3. Run
Terminal (inside the project folder, where `pom.xml` is):
```
mvn spring-boot:run
```
or in the IDE: open the folder as a Maven project, then run `SmartBenefitApplication`.
Open http://localhost:8080

## 4. Accounts
* Administrator (created automatically): `admin@smartbenefit.com` / `Admin@123`
* Citizen: click **Register** and create your own account.

## 5. Quick test (matches the worked examples)
1. Register -> log in -> **My Profile**: age 35, income 90000, Rural, Farmer, category SC, leave marital status empty -> Save.
2. **Check My Eligibility**: PMAY-G, MGNREGA, PM-KISAN are Eligible and ranked; Widow Pension is Not Eligible (age condition fails - this is shown even though marital status is empty).
3. **What-If Simulator**: set income 400000 -> PMAY-G changes to Not Eligible. The saved profile is unchanged.
4. Click **தமிழ்** at the top to switch to Tamil.
5. Log in as admin: change the ranking weights (they must add up to 1), add a scheme, add criteria - no code change needed.

## 6. Where is what (for the report)
| Report section | Code |
|---|---|
| 7.4 / 6.4 database tables | `model/*` (nine entities), tables created by Hibernate (`ddl-auto=update`) |
| 7.7.5 rule evaluation | `service/EligibilityService.java` (`evaluate`, `decide`, `matchScore`) |
| 7.7.6 scoring | `service/RecommendationService.java` (`rank`, `profileRelevance`) |
| 7.5 software reuse | `WhatIfService` calls `EligibilityService.evaluateAll` on a copy of the profile |
| Check = evaluate + store + rank (one transaction) | `service/EligibilityCheckService.java` |
| 7.7.4 security | `config/SecurityConfig.java`, `config/AppUserDetailsService.java` |
| Tamil / English | `config/WebConfig.java`, `resources/messages*.properties`, `Scheme.localName()` |
| Demo data (5 schemes, criteria, documents, steps) | `config/DataInitializer.java` |
| Admin panel | `controller/AdminController.java`, `templates/admin*.html` |

## 7. Things to know
* Benefit amounts and thresholds in `DataInitializer` are simplified for the prototype. Check them against the official scheme rules before presenting them as real.
* Eligibility explanations are generated in English; the interface text, scheme names and scheme descriptions are bilingual. The admin screens are English only.
* Sukanya Samriddhi is modelled on the girl child's profile (age 0-10) entered by the guardian, because the profile has no separate "child" fields.
* Reset everything: in MySQL Workbench run `DROP DATABASE smartbenefit;` and start the application again.
* Common problems: *Access denied for user root* -> wrong password in application.properties. *Port 8080 in use* -> change `server.port`. *Cannot find symbol getXxx()* in the IDE -> Lombok is not enabled.

## 7. Automated tests (JUnit 5)
The rule engine, ranking and what-if logic are covered by 25 JUnit tests in `src/test/java/com/smartbenefit/service`
(`EligibilityServiceTest`, `RecommendationServiceTest`, `WhatIfServiceTest`). They need no database. Run them with:
```
mvn test
```
In IntelliJ: right-click the `src/test/java` folder and choose *Run 'All Tests'*.
Each test name starts with the test case ID of the report (ET-01 ... ET-14, RT-01 ... RT-05, WT-01).

## New in this version

* **Gender** is now a profile field (Male, Female, Other, optional like the others). Rules can use it (parameter `gender`).
* **Import schemes from CSV**: Admin Panel, Import schemes (CSV). The whole file is validated first and nothing is saved if any row is wrong;
  a scheme whose name already exists is skipped.
* **Nine verified central schemes** are bundled in `src/main/resources/import/central-schemes.csv`
  (Atal Pension Yojana, PM-SYM, Ayushman Vay Vandana, PMAY-Urban 2.0, SC Post-Matric Scholarship, PM Vishwakarma, PMSBY, PMJJBY, PMUY).
  Each description ends with its official source link. Load them with the "Import the central schemes" button.
  Import them only after your earlier test evidence is finished: they change the ranking for the standard test profile.
* **New home page, colour theme, footer, scheme cards and an administrator side menu** (templates and CSS only; no logic changed).
* New automated tests: `SchemeImportServiceTest` (IM-01 to IM-12), `CentralSchemesDataTest` (CS-01 to CS-12), `GenderFieldTest` (GN-01 to GN-04).

Tamil text on the home page and in the imported schemes was written for this project and should be proofread.
