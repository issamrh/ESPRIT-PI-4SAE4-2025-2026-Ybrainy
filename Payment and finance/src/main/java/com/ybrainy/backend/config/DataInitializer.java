package com.ybrainy.backend.config;

import com.ybrainy.backend.entity.Course;
import com.ybrainy.backend.entity.Lesson;
import com.ybrainy.backend.entity.Meet;
import com.ybrainy.backend.entity.Report;
import com.ybrainy.backend.entity.CvSubmission;
import com.ybrainy.backend.entity.Certification;
import com.ybrainy.backend.entity.Quiz;
import com.ybrainy.backend.entity.Question;
import com.ybrainy.backend.repository.CourseRepository;
import com.ybrainy.backend.repository.LessonRepository;
import com.ybrainy.backend.repository.MeetRepository;
import com.ybrainy.backend.repository.ReportRepository;
import com.ybrainy.backend.repository.CvSubmissionRepository;
import com.ybrainy.backend.repository.CertificationRepository;
import com.ybrainy.backend.repository.QuizRepository;
import com.ybrainy.backend.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Populates the database with sample courses & lessons on every startup.
 * Videos use free, publicly-hosted MP4 URLs so they play immediately.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final CourseRepository courseRepo;
    private final LessonRepository lessonRepo;
    private final MeetRepository meetRepo;
    private final ReportRepository reportRepo;
    private final CvSubmissionRepository cvRepo;
    private final CertificationRepository certRepo;
    private final QuizRepository quizRepo;
    private final QuestionRepository questionRepo;

    /* ── Free sample video URLs (all public / royalty-free) ── */
    private static final String VID_BIG_BUCK   = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private static final String VID_ELEPHANT   = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4";
    private static final String VID_TEARS      = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4";
    private static final String VID_SINTEL     = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4";
    private static final String VID_SUBARU     = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4";
    private static final String VID_VOLKSWAGEN = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4";
    private static final String VID_FOR_BIGGER = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
    private static final String VID_FOR_ESCAPE = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4";
    private static final String VID_FOR_FUN    = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4";
    private static final String VID_FOR_JOY    = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4";

    public DataInitializer(CourseRepository courseRepo, LessonRepository lessonRepo, MeetRepository meetRepo,
                           ReportRepository reportRepo, CvSubmissionRepository cvRepo,
                           CertificationRepository certRepo, QuizRepository quizRepo, QuestionRepository questionRepo) {
        this.courseRepo = courseRepo;
        this.lessonRepo = lessonRepo;
        this.meetRepo = meetRepo;
        this.reportRepo = reportRepo;
        this.cvRepo = cvRepo;
        this.certRepo = certRepo;
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
    }

    @Override
    public void run(String... args) {

        // Only seed if the database is empty
        if (courseRepo.count() > 0) return;

        /* ═══════════════════════════════════════════════════════
         *  COURSE 1 — Web Development Bootcamp
         * ═══════════════════════════════════════════════════════ */
        Course web = courseRepo.save(Course.builder()
                .title("Full-Stack Web Development Bootcamp")
                .category("Web Development")
                .description("Master HTML, CSS, JavaScript, React, Node.js and build real-world projects from scratch.")
                .thumbnailVideoPath(VID_BIG_BUCK)
                .rating(4.8)
                .totalStudents(1245)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Introduction to HTML & the Web")
                .description("Learn how the internet works and write your first HTML page.")
                .videoPath(VID_BIG_BUCK)
                .duration("12 min")
                .orderIndex(0)
                .course(web)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("CSS Fundamentals & Flexbox")
                .description("Style pages with CSS, master Flexbox layout and responsive design.")
                .videoPath(VID_ELEPHANT)
                .duration("18 min")
                .orderIndex(1)
                .course(web)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("JavaScript Basics — Variables & Functions")
                .description("Core JavaScript concepts: variables, data types, functions and scope.")
                .videoPath(VID_FOR_FUN)
                .duration("22 min")
                .orderIndex(2)
                .course(web)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Building a REST API with Node.js")
                .description("Create an Express server, define routes and connect to a database.")
                .videoPath(VID_FOR_ESCAPE)
                .duration("30 min")
                .orderIndex(3)
                .course(web)
                .build());

        /* ═══════════════════════════════════════════════════════
         *  COURSE 2 — Data Science with Python
         * ═══════════════════════════════════════════════════════ */
        Course ds = courseRepo.save(Course.builder()
                .title("Data Science with Python")
                .category("Data Science")
                .description("From Pandas to Machine Learning — analyze data, build models and visualize insights.")
                .thumbnailVideoPath(VID_TEARS)
                .rating(4.6)
                .totalStudents(870)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Python Crash Course")
                .description("Quick refresher on Python syntax, loops, lists and dictionaries.")
                .videoPath(VID_TEARS)
                .duration("15 min")
                .orderIndex(0)
                .course(ds)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Data Wrangling with Pandas")
                .description("Load CSV files, clean data and perform aggregations with Pandas.")
                .videoPath(VID_FOR_BIGGER)
                .duration("25 min")
                .orderIndex(1)
                .course(ds)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Data Visualization — Matplotlib & Seaborn")
                .description("Create beautiful charts, histograms and heatmaps.")
                .videoPath(VID_FOR_JOY)
                .duration("20 min")
                .orderIndex(2)
                .course(ds)
                .build());

        /* ═══════════════════════════════════════════════════════
         *  COURSE 3 — Mobile App Development
         * ═══════════════════════════════════════════════════════ */
        Course mobile = courseRepo.save(Course.builder()
                .title("Mobile App Development with Flutter")
                .category("Mobile Development")
                .description("Build beautiful, natively compiled mobile apps for iOS and Android from a single codebase.")
                .thumbnailVideoPath(VID_SINTEL)
                .rating(4.7)
                .totalStudents(632)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Getting Started with Dart")
                .description("Learn Dart language fundamentals — the language behind Flutter.")
                .videoPath(VID_SINTEL)
                .duration("14 min")
                .orderIndex(0)
                .course(mobile)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Flutter Widgets Deep Dive")
                .description("Understand Stateless & Stateful widgets, layout and navigation.")
                .videoPath(VID_SUBARU)
                .duration("28 min")
                .orderIndex(1)
                .course(mobile)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("State Management with Provider")
                .description("Manage app state cleanly using the Provider package.")
                .videoPath(VID_VOLKSWAGEN)
                .duration("24 min")
                .orderIndex(2)
                .course(mobile)
                .build());

        /* ═══════════════════════════════════════════════════════
         *  COURSE 4 — UI/UX Design Essentials
         * ═══════════════════════════════════════════════════════ */
        Course design = courseRepo.save(Course.builder()
                .title("UI/UX Design Essentials")
                .category("Design")
                .description("Learn user-centered design principles, wireframing, prototyping and usability testing.")
                .thumbnailVideoPath(VID_VOLKSWAGEN)
                .rating(4.5)
                .totalStudents(415)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Design Thinking Process")
                .description("Empathize, Define, Ideate, Prototype, Test — the 5-step framework.")
                .videoPath(VID_VOLKSWAGEN)
                .duration("16 min")
                .orderIndex(0)
                .course(design)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Wireframing with Figma")
                .description("Hands-on wireframing, components and auto-layout in Figma.")
                .videoPath(VID_FOR_FUN)
                .duration("22 min")
                .orderIndex(1)
                .course(design)
                .build());

        /* ═══════════════════════════════════════════════════════
         *  COURSE 5 — Cybersecurity Fundamentals
         * ═══════════════════════════════════════════════════════ */
        Course cyber = courseRepo.save(Course.builder()
                .title("Cybersecurity Fundamentals")
                .category("Cybersecurity")
                .description("Understand threats, encryption, network security and ethical hacking basics.")
                .thumbnailVideoPath(VID_FOR_BIGGER)
                .rating(4.9)
                .totalStudents(980)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Introduction to Cybersecurity")
                .description("CIA triad, common attack vectors and the security landscape.")
                .videoPath(VID_FOR_BIGGER)
                .duration("10 min")
                .orderIndex(0)
                .course(cyber)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Encryption & Cryptography Basics")
                .description("Symmetric vs asymmetric encryption, hashing and digital signatures.")
                .videoPath(VID_ELEPHANT)
                .duration("19 min")
                .orderIndex(1)
                .course(cyber)
                .build());

        lessonRepo.save(Lesson.builder()
                .title("Network Security & Firewalls")
                .description("Secure network architecture, firewalls, IDS/IPS and VPNs.")
                .videoPath(VID_FOR_ESCAPE)
                .duration("26 min")
                .orderIndex(2)
                .course(cyber)
                .build());

        /* ═══════════════════════════════════════════════════════
         *  SAMPLE MEETS
         * ═══════════════════════════════════════════════════════ */
        LocalDateTime now = LocalDateTime.now();

        meetRepo.save(Meet.builder()
                .title("Web Dev Q&A Session")
                .description("Open Q&A about HTML, CSS and JavaScript fundamentals. Bring your questions!")
                .meetLink("https://meet.google.com/abc-defg-hij")
                .startTime(now.plusDays(1).withHour(10).withMinute(0))
                .endTime(now.plusDays(1).withHour(11).withMinute(0))
                .color("bg-primary")
                .course(web)
                .build());

        meetRepo.save(Meet.builder()
                .title("Data Science Workshop")
                .description("Live coding session: building a classification model with scikit-learn.")
                .meetLink("https://zoom.us/j/1234567890")
                .startTime(now.plusDays(2).withHour(14).withMinute(30))
                .endTime(now.plusDays(2).withHour(16).withMinute(0))
                .color("bg-success")
                .course(ds)
                .build());

        meetRepo.save(Meet.builder()
                .title("Flutter UI Challenge")
                .description("Build a beautiful login screen together in 30 minutes.")
                .meetLink("https://teams.microsoft.com/l/meetup-join/example")
                .startTime(now.plusDays(3).withHour(9).withMinute(0))
                .endTime(now.plusDays(3).withHour(9).withMinute(45))
                .color("bg-warning")
                .course(mobile)
                .build());

        meetRepo.save(Meet.builder()
                .title("Cybersecurity Office Hours")
                .description("Weekly drop-in session for cybersecurity course students.")
                .meetLink("https://meet.google.com/xyz-uvwx-rst")
                .startTime(now.plusDays(5).withHour(16).withMinute(0))
                .endTime(now.plusDays(5).withHour(17).withMinute(0))
                .color("bg-danger")
                .course(cyber)
                .build());

        meetRepo.save(Meet.builder()
                .title("General Instructor Meeting")
                .description("Monthly all-hands meeting for all instructors.")
                .meetLink("https://zoom.us/j/9876543210")
                .startTime(now.plusDays(7).withHour(11).withMinute(0))
                .endTime(now.plusDays(7).withHour(12).withMinute(0))
                .color("bg-info")
                .build());

        /* ═══════════════════════════════════════════════════════
         *  SAMPLE REPORTS — Student
         * ═══════════════════════════════════════════════════════ */
        reportRepo.save(Report.builder()
                .type("student").title("Low Attendance in Web Dev Course")
                .description("Student has attended only 3 out of 12 scheduled sessions. Participation is critically low and may affect certification eligibility.")
                .category("attendance").priority("high").status("open")
                .subjectName("Ahmed Ben Salah").subjectEmail("ahmed.bs@ybrainy.com")
                .submittedBy("Prof. Karim Trabelsi").courseName("Full-Stack Web Development Bootcamp")
                .build());

        reportRepo.save(Report.builder()
                .type("student").title("Certification Exam — Suspected Plagiarism")
                .description("Final certification answers show 92% similarity with another student's submission. Investigation required before issuing certificate.")
                .category("certification").priority("critical").status("in_progress")
                .subjectName("Sarra Khelifi").subjectEmail("sarra.k@ybrainy.com")
                .submittedBy("Dr. Amira Bouazizi").certificationName("Data Science Professional Certificate")
                .build());

        reportRepo.save(Report.builder()
                .type("student").title("Outstanding Performance — Flutter Course")
                .description("Student completed all assignments ahead of schedule with top marks. Recommended for advanced certification track and mentorship program.")
                .category("performance").priority("low").status("resolved")
                .subjectName("Mohamed Gharbi").subjectEmail("med.gharbi@ybrainy.com")
                .submittedBy("Prof. Leila Hamdi").courseName("Mobile App Development with Flutter")
                .certificationName("Flutter Developer Certificate")
                .build());

        reportRepo.save(Report.builder()
                .type("student").title("Disruptive Behavior in Live Q&A Sessions")
                .description("Student has repeatedly disrupted online Q&A sessions with off-topic questions and inappropriate comments. Warning has been issued.")
                .category("behavior").priority("medium").status("open")
                .subjectName("Youssef Mansouri").subjectEmail("youssef.m@ybrainy.com")
                .submittedBy("Prof. Karim Trabelsi").courseName("Full-Stack Web Development Bootcamp")
                .build());

        reportRepo.save(Report.builder()
                .type("student").title("Unable to Access Certification Portal")
                .description("Student reports persistent login issues when attempting to access the certification exam portal. Technical support escalation needed.")
                .category("issue").priority("high").status("in_progress")
                .subjectName("Fatma Riahi").subjectEmail("fatma.r@ybrainy.com")
                .submittedBy("System Auto-Report").certificationName("Cybersecurity Fundamentals Certificate")
                .build());

        reportRepo.save(Report.builder()
                .type("student").title("Course Feedback — Excellent UI/UX Content")
                .description("Student praised the Figma wireframing module and suggested adding more real-world case studies. Overall rating: 5/5.")
                .category("feedback").priority("low").status("closed")
                .subjectName("Ines Bouzid").subjectEmail("ines.b@ybrainy.com")
                .submittedBy("Ines Bouzid").courseName("UI/UX Design Essentials")
                .build());

        /* ═══════════════════════════════════════════════════════
         *  SAMPLE REPORTS — Teacher
         * ═══════════════════════════════════════════════════════ */
        reportRepo.save(Report.builder()
                .type("teacher").title("Late Grading of Assignments")
                .description("Multiple students have complained that assignments submitted 3 weeks ago are still ungraded. This delays certification progress.")
                .category("performance").priority("high").status("open")
                .subjectName("Prof. Karim Trabelsi").subjectEmail("karim.t@ybrainy.com")
                .submittedBy("Academic Affairs Office").courseName("Full-Stack Web Development Bootcamp")
                .build());

        reportRepo.save(Report.builder()
                .type("teacher").title("Exceptional Student Satisfaction Scores")
                .description("Teacher received 4.9/5 average rating across all courses this semester. Students highlighted clear explanations and responsive communication.")
                .category("performance").priority("low").status("closed")
                .subjectName("Dr. Amira Bouazizi").subjectEmail("amira.b@ybrainy.com")
                .submittedBy("Quality Assurance Team").courseName("Data Science with Python")
                .build());

        reportRepo.save(Report.builder()
                .type("teacher").title("Inappropriate Language During Live Session")
                .description("A student reported that the instructor used unprofessional language during a live coding session. Review of session recording is pending.")
                .category("behavior").priority("critical").status("in_progress")
                .subjectName("Mr. Nabil Chaabane").subjectEmail("nabil.c@ybrainy.com")
                .submittedBy("Student Affairs Committee").courseName("Cybersecurity Fundamentals")
                .build());

        reportRepo.save(Report.builder()
                .type("teacher").title("Certification Material Not Updated")
                .description("The Flutter certification exam references deprecated APIs (Flutter 2.x). Material needs to be updated to Flutter 3.x standards.")
                .category("issue").priority("medium").status("open")
                .subjectName("Prof. Leila Hamdi").subjectEmail("leila.h@ybrainy.com")
                .submittedBy("Curriculum Review Board").courseName("Mobile App Development with Flutter")
                .certificationName("Flutter Developer Certificate")
                .build());

        reportRepo.save(Report.builder()
                .type("teacher").title("Request for Additional Teaching Hours")
                .description("Teacher has requested 4 extra hours per week to accommodate the growing enrollment in the Data Science course. Budget approval needed.")
                .category("feedback").priority("medium").status("open")
                .subjectName("Dr. Amira Bouazizi").subjectEmail("amira.b@ybrainy.com")
                .submittedBy("Dr. Amira Bouazizi").courseName("Data Science with Python")
                .build());

        /* ═══════════════════════════════════════════════════════
         *  SAMPLE CV SUBMISSIONS
         * ═══════════════════════════════════════════════════════ */
        cvRepo.save(CvSubmission.builder()
                .fullName("Ahmed Ben Salah").email("ahmed.bs@gmail.com").phone("+216 55 123 456")
                .position("Junior Full-Stack Developer").courseName("Full-Stack Web Development Bootcamp")
                .educationLevel("bachelor").yearsOfExperience(1)
                .skills("JavaScript, React, Node.js, Spring Boot, MySQL")
                .coverLetter("I recently completed the Full-Stack Web Development Bootcamp at YBrainy and I am eager to apply my skills in a professional environment. During the course I built 3 full-stack projects including an e-commerce platform.")
                .status("pending").build());

        cvRepo.save(CvSubmission.builder()
                .fullName("Sarra Khelifi").email("sarra.k@outlook.com").phone("+216 98 765 432")
                .position("Data Scientist Intern").courseName("Data Science with Python")
                .educationLevel("master").yearsOfExperience(0)
                .skills("Python, Pandas, Scikit-learn, TensorFlow, SQL, Tableau")
                .coverLetter("As a Master's student in Applied Mathematics, I have complemented my theoretical foundation with the YBrainy Data Science certification. I am looking for an internship to apply machine learning models to real business problems.")
                .status("reviewed").build());

        cvRepo.save(CvSubmission.builder()
                .fullName("Mohamed Gharbi").email("med.gharbi@gmail.com").phone("+216 22 333 444")
                .position("Mobile App Developer").courseName("Mobile App Development with Flutter")
                .educationLevel("bachelor").yearsOfExperience(2)
                .skills("Flutter, Dart, Firebase, REST APIs, Git, Figma")
                .coverLetter("With 2 years of freelance experience building Flutter apps and a YBrainy Flutter Developer Certificate, I am ready for a full-time mobile development role. My portfolio includes 5 published apps on Google Play Store.")
                .status("shortlisted").build());

        cvRepo.save(CvSubmission.builder()
                .fullName("Fatma Riahi").email("fatma.r@yahoo.com").phone("+216 50 111 222")
                .position("Cybersecurity Analyst").courseName("Cybersecurity Fundamentals")
                .educationLevel("master").yearsOfExperience(3)
                .skills("Penetration Testing, Wireshark, Metasploit, Linux, Network Security, SIEM")
                .coverLetter("I hold a Master's degree in Information Security and the YBrainy Cybersecurity Fundamentals Certificate. With 3 years of experience in SOC operations, I am seeking a senior analyst role where I can lead incident response efforts.")
                .status("accepted").build());

        cvRepo.save(CvSubmission.builder()
                .fullName("Youssef Mansouri").email("youssef.m@gmail.com").phone("+216 99 888 777")
                .position("UI/UX Designer").courseName("UI/UX Design Essentials")
                .educationLevel("diploma").yearsOfExperience(1)
                .skills("Figma, Adobe XD, Sketch, HTML/CSS, User Research, Wireframing")
                .coverLetter("I am a creative designer who recently earned the YBrainy UI/UX Design certificate. I have redesigned 2 mobile apps during the course and received positive feedback from instructors and peers.")
                .status("pending").build());

        cvRepo.save(CvSubmission.builder()
                .fullName("Ines Bouzid").email("ines.b@hotmail.com").phone("+216 55 999 666")
                .position("Full-Stack Developer").courseName("Full-Stack Web Development Bootcamp")
                .educationLevel("bachelor").yearsOfExperience(0)
                .skills("Java, Spring Boot, Angular, PostgreSQL, Docker")
                .coverLetter("Fresh graduate with a strong passion for backend development. The YBrainy bootcamp gave me hands-on experience with enterprise-grade Spring Boot applications. Looking for my first professional opportunity.")
                .status("rejected").build());

        cvRepo.save(CvSubmission.builder()
                .fullName("Karim Trabelsi Jr.").email("karim.jr@gmail.com").phone("+216 23 456 789")
                .position("Machine Learning Engineer").courseName("Data Science with Python")
                .educationLevel("phd").yearsOfExperience(5)
                .skills("Python, PyTorch, Deep Learning, NLP, Computer Vision, MLOps, AWS SageMaker")
                .coverLetter("PhD researcher with 5 years of experience in NLP and computer vision. Published 4 papers in top-tier conferences. Completed YBrainy's advanced certification to stay current with industry best practices.")
                .status("shortlisted").build());

        cvRepo.save(CvSubmission.builder()
                .fullName("Nour Chaabane").email("nour.c@gmail.com").phone("+216 91 234 567")
                .position("Teaching Assistant").courseName("Mobile App Development with Flutter")
                .educationLevel("master").yearsOfExperience(2)
                .skills("Flutter, Dart, Teaching, Mentoring, Technical Writing, Git")
                .coverLetter("I am passionate about both mobile development and education. With my YBrainy Flutter certificate and 2 years of tutoring experience, I would love to join YBrainy as a teaching assistant to help new students succeed.")
                .status("pending").build());

        /* ═══════════════════════════════════════════════════════
         *  SAMPLE CERTIFICATIONS
         * ═══════════════════════════════════════════════════════ */
        Certification certWeb = certRepo.save(Certification.builder()
                .title("Full-Stack Web Development Certificate")
                .description("Validates proficiency in HTML, CSS, JavaScript, React, Node.js, and database management. Students must complete all course modules and pass the final assessment with 70% or higher.")
                .issuedBy("YBrainy Academy").category("Web Development").level("intermediate")
                .duration("12 weeks").passingScore(70).earnedCount(245)
                .prerequisites("Basic HTML/CSS knowledge, familiarity with any programming language")
                .badgeImageUrl("https://img.icons8.com/fluency/96/certificate.png")
                .status("active").course(web).build());

        Certification certDS = certRepo.save(Certification.builder()
                .title("Data Science Professional Certificate")
                .description("Comprehensive certification covering Python, Pandas, Machine Learning, Deep Learning, and statistical analysis. Prepares students for real-world data science roles.")
                .issuedBy("YBrainy Academy").category("Data Science").level("advanced")
                .duration("16 weeks").passingScore(75).earnedCount(182)
                .prerequisites("Python fundamentals, basic statistics and linear algebra")
                .badgeImageUrl("https://img.icons8.com/fluency/96/combo-chart.png")
                .status("active").course(ds).build());

        Certification certFlutter = certRepo.save(Certification.builder()
                .title("Flutter Mobile Developer Certificate")
                .description("Certifies ability to build cross-platform mobile applications using Flutter and Dart. Covers UI design, state management, API integration, and deployment to App Store/Play Store.")
                .issuedBy("YBrainy Academy").category("Mobile Development").level("intermediate")
                .duration("10 weeks").passingScore(70).earnedCount(156)
                .prerequisites("Object-oriented programming, basic mobile development concepts")
                .badgeImageUrl("https://img.icons8.com/fluency/96/flutter.png")
                .status("active").course(mobile).build());

        Certification certCyber = certRepo.save(Certification.builder()
                .title("Cybersecurity Fundamentals Certificate")
                .description("Covers network security, ethical hacking, penetration testing, cryptography, and incident response. Aligned with industry standards like CompTIA Security+.")
                .issuedBy("YBrainy Academy").category("Cybersecurity").level("beginner")
                .duration("8 weeks").passingScore(80).earnedCount(98)
                .prerequisites("Basic networking knowledge (TCP/IP, DNS, HTTP)")
                .badgeImageUrl("https://img.icons8.com/fluency/96/security-checked.png")
                .status("active").course(cyber).build());

        Certification certUX = certRepo.save(Certification.builder()
                .title("UI/UX Design Essentials Certificate")
                .description("Validates skills in user research, wireframing, prototyping, visual design, and usability testing using tools like Figma and Adobe XD.")
                .issuedBy("YBrainy Academy").category("Design").level("beginner")
                .duration("6 weeks").passingScore(65).earnedCount(210)
                .prerequisites("No prerequisites — open to all creative individuals")
                .badgeImageUrl("https://img.icons8.com/fluency/96/design.png")
                .status("active").course(design).build());

        Certification certDraft = certRepo.save(Certification.builder()
                .title("Cloud Computing with AWS (Coming Soon)")
                .description("Learn AWS services including EC2, S3, Lambda, DynamoDB, and CloudFormation. Build and deploy scalable cloud solutions.")
                .issuedBy("YBrainy Academy").category("Cloud Computing").level("advanced")
                .duration("14 weeks").passingScore(75).earnedCount(0)
                .prerequisites("Linux basics, networking fundamentals, any programming language")
                .badgeImageUrl("https://img.icons8.com/fluency/96/cloud.png")
                .status("draft").build());

        /* ═══════════════════════════════════════════════════════
         *  SAMPLE QUIZZES & QUESTIONS
         * ═══════════════════════════════════════════════════════ */

        // --- Quiz 1: Web Dev Final Assessment ---
        Quiz quizWeb = quizRepo.save(Quiz.builder()
                .title("Full-Stack Web Development — Final Assessment")
                .description("Comprehensive quiz covering HTML, CSS, JavaScript, React, Node.js, and REST APIs. You must score 70% or higher to earn your certificate.")
                .certification(certWeb).timeLimit(60).passingScore(70).maxAttempts(3)
                .difficulty("medium").status("published").attemptCount(312).averageScore(76.5)
                .build());

        questionRepo.save(Question.builder().quiz(quizWeb).orderIndex(0).points(2)
                .questionText("Which HTML5 element is used to define navigation links?")
                .optionA("<nav>").optionB("<navigation>").optionC("<links>").optionD("<menu>")
                .correctAnswer("A").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizWeb).orderIndex(1).points(2)
                .questionText("In CSS, what does 'display: flex' do?")
                .optionA("Makes the element invisible").optionB("Creates a flexible box layout container")
                .optionC("Fixes the element position").optionD("Stretches the element to full width")
                .correctAnswer("B").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizWeb).orderIndex(2).points(2)
                .questionText("What is the correct way to declare a constant in JavaScript ES6?")
                .optionA("var x = 5").optionB("let x = 5").optionC("const x = 5").optionD("constant x = 5")
                .correctAnswer("C").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizWeb).orderIndex(3).points(3)
                .questionText("In React, which hook is used to manage component state?")
                .optionA("useEffect").optionB("useContext").optionC("useReducer").optionD("useState")
                .correctAnswer("D").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizWeb).orderIndex(4).points(3)
                .questionText("What HTTP method is typically used to update an existing resource in a REST API?")
                .optionA("GET").optionB("POST").optionC("PUT").optionD("DELETE")
                .correctAnswer("C").questionType("multiple_choice").build());

        // --- Quiz 2: Data Science Mid-Term ---
        Quiz quizDS = quizRepo.save(Quiz.builder()
                .title("Data Science — Python & Machine Learning Quiz")
                .description("Test your knowledge of Python data analysis, Pandas, Scikit-learn, and machine learning fundamentals.")
                .certification(certDS).timeLimit(45).passingScore(75).maxAttempts(2)
                .difficulty("hard").status("published").attemptCount(198).averageScore(68.3)
                .build());

        questionRepo.save(Question.builder().quiz(quizDS).orderIndex(0).points(2)
                .questionText("Which Python library is primarily used for data manipulation and analysis?")
                .optionA("NumPy").optionB("Pandas").optionC("Matplotlib").optionD("Scikit-learn")
                .correctAnswer("B").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizDS).orderIndex(1).points(2)
                .questionText("What type of machine learning is 'Linear Regression'?")
                .optionA("Unsupervised Learning").optionB("Reinforcement Learning")
                .optionC("Supervised Learning").optionD("Semi-supervised Learning")
                .correctAnswer("C").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizDS).orderIndex(2).points(3)
                .questionText("Which metric is best suited for evaluating a classification model with imbalanced classes?")
                .optionA("Accuracy").optionB("F1 Score").optionC("Mean Squared Error").optionD("R² Score")
                .correctAnswer("B").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizDS).orderIndex(3).points(2)
                .questionText("What does the 'fit()' method do in Scikit-learn?")
                .optionA("Makes predictions").optionB("Transforms data")
                .optionC("Trains the model on data").optionD("Evaluates model performance")
                .correctAnswer("C").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizDS).orderIndex(4).points(3)
                .questionText("In a Neural Network, what is the purpose of an activation function?")
                .optionA("To initialize weights").optionB("To introduce non-linearity")
                .optionC("To normalize input data").optionD("To reduce overfitting")
                .correctAnswer("B").questionType("multiple_choice").build());

        // --- Quiz 3: Flutter Basics ---
        Quiz quizFlutter = quizRepo.save(Quiz.builder()
                .title("Flutter & Dart — Fundamentals Quiz")
                .description("Assess your understanding of Dart language basics, Flutter widgets, state management, and navigation.")
                .certification(certFlutter).timeLimit(30).passingScore(70).maxAttempts(3)
                .difficulty("easy").status("published").attemptCount(245).averageScore(82.1)
                .build());

        questionRepo.save(Question.builder().quiz(quizFlutter).orderIndex(0).points(1)
                .questionText("What programming language does Flutter use?")
                .optionA("Kotlin").optionB("Swift").optionC("Dart").optionD("JavaScript")
                .correctAnswer("C").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizFlutter).orderIndex(1).points(2)
                .questionText("Which widget is used to create a scrollable list of items in Flutter?")
                .optionA("Column").optionB("ListView").optionC("Stack").optionD("GridView")
                .correctAnswer("B").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizFlutter).orderIndex(2).points(2)
                .questionText("What is the difference between StatelessWidget and StatefulWidget?")
                .optionA("StatelessWidget can change over time").optionB("StatefulWidget cannot be rebuilt")
                .optionC("StatefulWidget maintains mutable state").optionD("There is no difference")
                .correctAnswer("C").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizFlutter).orderIndex(3).points(2)
                .questionText("Which state management solution is officially recommended by the Flutter team?")
                .optionA("Redux").optionB("Provider").optionC("MobX").optionD("Bloc only")
                .correctAnswer("B").questionType("multiple_choice").build());

        // --- Quiz 4: Cybersecurity ---
        Quiz quizCyber = quizRepo.save(Quiz.builder()
                .title("Cybersecurity Fundamentals — Assessment")
                .description("Evaluate your knowledge of network security, encryption, ethical hacking, and incident response procedures.")
                .certification(certCyber).timeLimit(40).passingScore(80).maxAttempts(2)
                .difficulty("medium").status("published").attemptCount(120).averageScore(71.8)
                .build());

        questionRepo.save(Question.builder().quiz(quizCyber).orderIndex(0).points(2)
                .questionText("What does SQL injection attack target?")
                .optionA("Network layer").optionB("Database layer").optionC("Physical layer").optionD("Transport layer")
                .correctAnswer("B").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizCyber).orderIndex(1).points(2)
                .questionText("Which protocol provides secure communication over the internet?")
                .optionA("HTTP").optionB("FTP").optionC("HTTPS/TLS").optionD("SMTP")
                .correctAnswer("C").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizCyber).orderIndex(2).points(3)
                .questionText("What is the primary purpose of a firewall?")
                .optionA("Encrypt data").optionB("Filter network traffic based on rules")
                .optionC("Detect malware").optionD("Backup data")
                .correctAnswer("B").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizCyber).orderIndex(3).points(2)
                .questionText("What type of attack involves overwhelming a server with excessive requests?")
                .optionA("Phishing").optionB("Man-in-the-Middle").optionC("DDoS").optionD("Brute Force")
                .correctAnswer("C").questionType("multiple_choice").build());

        // --- Quiz 5: UX Design (Draft) ---
        Quiz quizUX = quizRepo.save(Quiz.builder()
                .title("UI/UX Design — Practice Quiz")
                .description("Test your understanding of user experience principles, design thinking, wireframing, and prototyping.")
                .certification(certUX).timeLimit(25).passingScore(65).maxAttempts(5)
                .difficulty("easy").status("draft").attemptCount(0).averageScore(0.0)
                .build());

        questionRepo.save(Question.builder().quiz(quizUX).orderIndex(0).points(1)
                .questionText("What is the first step in the Design Thinking process?")
                .optionA("Prototype").optionB("Define").optionC("Empathize").optionD("Test")
                .correctAnswer("C").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizUX).orderIndex(1).points(2)
                .questionText("What is a wireframe?")
                .optionA("A high-fidelity design mockup").optionB("A low-fidelity structural layout of a page")
                .optionC("A coded prototype").optionD("A user flow diagram")
                .correctAnswer("B").questionType("multiple_choice").build());
        questionRepo.save(Question.builder().quiz(quizUX).orderIndex(2).points(2)
                .questionText("Which tool is most commonly used for collaborative UI design?")
                .optionA("Photoshop").optionB("Microsoft Word").optionC("Figma").optionD("Excel")
                .correctAnswer("C").questionType("multiple_choice").build());

        System.out.println("✅ Sample data loaded: 5 courses, 14 lessons, 5 meets, 11 reports, 8 CVs, 6 certifications, 5 quizzes, 22 questions");
    }
}

