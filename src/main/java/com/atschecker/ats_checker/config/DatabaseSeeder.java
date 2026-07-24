package com.atschecker.ats_checker.config;

import com.atschecker.ats_checker.entity.Specialization;
import com.atschecker.ats_checker.entity.SpecializationSkill;
import com.atschecker.ats_checker.entity.User;
import com.atschecker.ats_checker.repository.SpecializationRepository;
import com.atschecker.ats_checker.repository.SpecializationSkillRepository;
import com.atschecker.ats_checker.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final SpecializationRepository specializationRepository;
    private final SpecializationSkillRepository skillRepository;
    private final UserRepository userRepository;

    public DatabaseSeeder(SpecializationRepository specializationRepository,
                          SpecializationSkillRepository skillRepository,
                          UserRepository userRepository) {
        this.specializationRepository = specializationRepository;
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (specializationRepository.count() == 0) {
            seedSpecializations();
        }
        seedDefaultUsers();
    }

    private void seedDefaultUsers() {
        // Seed admin user if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            String hashed = BCrypt.hashpw("admin123", BCrypt.gensalt(10));
            userRepository.save(new User("admin", hashed, "admin@atschecker.com", "ADMIN"));
            System.out.println("[Seeder] Default admin created  → username: admin  / password: admin123");
        }
        // Seed test user if not exists
        if (userRepository.findByUsername("test").isEmpty()) {
            String hashed = BCrypt.hashpw("test123", BCrypt.gensalt(10));
            userRepository.save(new User("test", hashed, "test@atschecker.com", "USER"));
            System.out.println("[Seeder] Default test user created → username: test  / password: test123");
        }
    }

    private void seedSpecializations() {
        Map<String, String[]> techSkillsMap = new LinkedHashMap<>();
        Map<String, String[]> softSkillsMap = new LinkedHashMap<>();
        Map<String, String> descMap = new LinkedHashMap<>();

        // Java Developer
        descMap.put("Java Developer", "Evaluates core Java backend concepts, libraries, frameworks, build tools, and design principles.");
        techSkillsMap.put("Java Developer", new String[]{
                "Java", "OOP", "Collections", "Multithreading", "JDBC", "Spring Boot", "Hibernate", "REST API", "Maven", "Git", "SQL", "MySQL", "Exception Handling", "Design Patterns"
        });
        softSkillsMap.put("Java Developer", new String[]{"Problem Solving", "Analytical Thinking", "Technical Writing"});

        // Full Stack Java Developer
        descMap.put("Full Stack Java Developer", "Evaluates both Spring Boot backend skills and modern Javascript/CSS frontend concepts.");
        techSkillsMap.put("Full Stack Java Developer", new String[]{
                "Java", "Spring Boot", "Hibernate", "REST API", "HTML5", "CSS3", "JavaScript", "React", "Angular", "Vue", "MySQL", "JPA", "Maven", "Git", "Docker", "System Design"
        });
        softSkillsMap.put("Full Stack Java Developer", new String[]{"Communication", "Teamwork", "Adaptability"});

        // Backend Developer
        descMap.put("Backend Developer", "Evaluates backend design, microservices, databases, caching, and server-side runtimes.");
        techSkillsMap.put("Backend Developer", new String[]{
                "Java", "Python", "Go", "Node.js", "Spring Boot", "REST API", "PostgreSQL", "MySQL", "Redis", "Docker", "System Design", "Git", "Microservices", "API Gateway"
        });
        softSkillsMap.put("Backend Developer", new String[]{"Critical Thinking", "Problem Solving", "Time Management"});

        // Cloud Engineer
        descMap.put("Cloud Engineer", "Evaluates cloud architecture, IAC tools, provisioning, security, and cloud providers.");
        techSkillsMap.put("Cloud Engineer", new String[]{
                "AWS", "Azure", "GCP", "Terraform", "CloudFormation", "Docker", "Kubernetes", "Serverless", "IAM", "VPC", "CI/CD", "Git", "Linux", "Cloud Security"
        });
        softSkillsMap.put("Cloud Engineer", new String[]{"Adaptability", "Collaboration", "Continuous Learning"});

        // DevOps Engineer
        descMap.put("DevOps Engineer", "Evaluates automation, infrastructure, system admin, monitoring, and pipeline tools.");
        techSkillsMap.put("DevOps Engineer", new String[]{
                "Jenkins", "CI/CD", "Docker", "Kubernetes", "Ansible", "Terraform", "Git", "Linux", "Bash", "Prometheus", "Grafana", "AWS", "Nginx", "Shell Scripting"
        });
        softSkillsMap.put("DevOps Engineer", new String[]{"Problem Solving", "Teamwork", "Collaboration"});

        // Software Engineer
        descMap.put("Software Engineer", "Evaluates core computer science fundamentals, programming languages, and SDLC.");
        techSkillsMap.put("Software Engineer", new String[]{
                "Java", "Python", "C++", "Data Structures", "Algorithms", "OOP", "System Design", "Git", "Agile", "SQL", "Software Development Life Cycle (SDLC)", "Unit Testing", "Maven"
        });
        softSkillsMap.put("Software Engineer", new String[]{"Problem Solving", "Analytical Thinking", "Communication"});

        // Data Analyst
        descMap.put("Data Analyst", "Evaluates SQL database queries, python analysis libraries, and visualization tools.");
        techSkillsMap.put("Data Analyst", new String[]{
                "Python", "SQL", "Excel", "Tableau", "Power BI", "Pandas", "NumPy", "Statistics", "Data Visualization", "Data Cleaning", "R", "ETL", "Data Warehousing"
        });
        softSkillsMap.put("Data Analyst", new String[]{"Attention to Detail", "Business Acumen", "Presentation Skills"});

        // Frontend Developer
        descMap.put("Frontend Developer", "Evaluates web layouts, modern JavaScript framework capabilities, and UI libraries.");
        techSkillsMap.put("Frontend Developer", new String[]{
                "HTML5", "CSS3", "JavaScript", "TypeScript", "React", "Angular", "Vue", "Tailwind CSS", "Bootstrap", "Webpack", "Responsive Design", "Git", "Fetch API", "DOM Manipulation"
        });
        softSkillsMap.put("Frontend Developer", new String[]{"Design Aesthetic", "User Empathy", "Collaboration"});

        // Android Developer
        descMap.put("Android Developer", "Evaluates mobile development paradigms, Kotlin/Java Android APIs, and MVVM patterns.");
        techSkillsMap.put("Android Developer", new String[]{
                "Kotlin", "Java", "Android SDK", "Android Studio", "Jetpack Compose", "XML", "Retrofit", "Room", "MVVM", "Git", "REST API", "Gradle", "Coroutines"
        });
        softSkillsMap.put("Android Developer", new String[]{"Problem Solving", "Creativity", "Time Management"});

        // Cyber Security
        descMap.put("Cyber Security", "Evaluates security concepts, network protocols, penetration testing, and identity management.");
        techSkillsMap.put("Cyber Security", new String[]{
                "Cryptography", "Network Security", "Penetration Testing", "Ethical Hacking", "OWASP Top 10", "Firewalls", "Wireshark", "Linux", "Vulnerability Assessment", "IAM", "Incident Response"
            });
        softSkillsMap.put("Cyber Security", new String[]{"Ethical Judgement", "Analytical Thinking", "Attention to Detail"});

        // Iterate and save
        for (String specName : descMap.keySet()) {
            Specialization spec = new Specialization(specName, descMap.get(specName));
            spec = specializationRepository.save(spec);

            String[] tech = techSkillsMap.get(specName);
            for (String t : tech) {
                skillRepository.save(new SpecializationSkill(spec, t, "TECHNICAL"));
            }

            String[] soft = softSkillsMap.get(specName);
            for (String s : soft) {
                skillRepository.save(new SpecializationSkill(spec, s, "SOFT"));
            }
        }
    }
}
