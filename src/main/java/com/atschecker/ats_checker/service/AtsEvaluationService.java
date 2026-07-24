package com.atschecker.ats_checker.service;

import com.atschecker.ats_checker.entity.AtsResult;
import com.atschecker.ats_checker.entity.Resume;
import com.atschecker.ats_checker.entity.Specialization;
import com.atschecker.ats_checker.entity.SpecializationSkill;
import com.atschecker.ats_checker.repository.SpecializationSkillRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AtsEvaluationService {

    private static final Logger log = Logger.getLogger(AtsEvaluationService.class.getName());

    private final SpecializationSkillRepository skillRepository;
    private final GeminiService geminiService;

    // Stop words for JD keyword extraction
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "of", "to", "in", "is", "that", "it", "on", "for", "with", "as", "at", "by", "an", "be",
            "this", "are", "from", "or", "have", "you", "your", "our", "we", "required", "preferred", "experience",
            "candidate", "role", "work", "team", "skills", "job", "description", "ability", "years", "knowledge",
            "must", "should", "will", "development", "developer", "engineer", "software", "system", "using", "working"
    );

    // Self-caching pattern map for short alphanumeric acronyms to avoid false-positive substring matches
    private static final Map<String, Pattern> ACRONYM_PATTERNS = new java.util.concurrent.ConcurrentHashMap<>();

    public AtsEvaluationService(SpecializationSkillRepository skillRepository, GeminiService geminiService) {
        this.skillRepository = skillRepository;
        this.geminiService = geminiService;
    }

    public AtsResult evaluate(Resume resume, Specialization specialization, String jobDescription) {
        String resumeText = resume.getRawText();
        String normalizedResume = resumeText.toLowerCase().replaceAll("\\s+", " ");
        String specName = specialization != null ? specialization.getName() : "Software Engineering";

        List<String> targetSkills = new ArrayList<>();
        List<String> softSkills = new ArrayList<>();

        // 1. Gather Specialization Skills from DB
        if (specialization != null) {
            List<SpecializationSkill> specSkills = skillRepository.findBySpecialization(specialization);
            for (SpecializationSkill skill : specSkills) {
                if ("SOFT".equalsIgnoreCase(skill.getSkillCategory())) {
                    softSkills.add(skill.getSkillName());
                } else {
                    targetSkills.add(skill.getSkillName());
                }
            }
        }

        // 2. Extract JD Keywords if present
        List<String> jdKeywords = new ArrayList<>();
        if (jobDescription != null && !jobDescription.trim().isEmpty()) {
            jdKeywords = extractKeywordsFromJd(jobDescription);
            targetSkills.addAll(jdKeywords);
            // Deduplicate
            targetSkills = targetSkills.stream().distinct().collect(Collectors.toList());
        }

        // Default list of soft skills if specialization has none
        if (softSkills.isEmpty()) {
            softSkills.addAll(Arrays.asList("Communication", "Leadership", "Teamwork", "Problem Solving", "Time Management", "Adaptability"));
        }

        // 3. Match Skills
        List<String> matchedTech = new ArrayList<>();
        List<String> missingTech = new ArrayList<>();
        for (String skill : targetSkills) {
            if (isSkillPresent(normalizedResume, skill)) {
                matchedTech.add(skill);
            } else {
                missingTech.add(skill);
            }
        }

        List<String> matchedSoft = new ArrayList<>();
        List<String> missingSoft = new ArrayList<>();
        for (String skill : softSkills) {
            if (isSkillPresent(normalizedResume, skill)) {
                matchedSoft.add(skill);
            } else {
                missingSoft.add(skill);
            }
        }

        // Combine into CSV or JSON strings for storage
        String matchingSkillsStr = String.join(", ", matchedTech) + (matchedSoft.isEmpty() ? "" : " | Soft: " + String.join(", ", matchedSoft));
        String missingSkillsStr = String.join(", ", missingTech) + (missingSoft.isEmpty() ? "" : " | Soft: " + String.join(", ", missingSoft));

        // 4. Calculate Section Scores
        // Tech skill coverage (35% weight) — uses sqrt curve to reward partial matches
        double techCoverage = targetSkills.isEmpty() ? 0.5
                : (double) matchedTech.size() / targetSkills.size();
        int techScore = (int) (Math.sqrt(techCoverage) * 100); // sqrt curve: 64% match → 80 score

        // Soft skill coverage (10% weight)
        double softCoverage = softSkills.isEmpty() ? 0.5
                : (double) matchedSoft.size() / softSkills.size();
        int softScore = (int) (softCoverage * 100);

        // Combined keyword coverage for display (still reported as-is)
        int totalSkills = targetSkills.size() + softSkills.size();
        int matchedCount = matchedTech.size() + matchedSoft.size();
        double keywordCoverage = totalSkills > 0 ? (double) matchedCount / totalSkills : 0.0;

        // Experience match score (20% weight)
        int experienceScore = evaluateExperience(normalizedResume);

        // Education match score (15% weight)
        int educationScore = evaluateEducation(normalizedResume);

        // Projects match score (10% weight)
        int projectsScore = evaluateProjects(normalizedResume);

        // Certifications match score (10% weight)
        int certsCount = countCertifications(normalizedResume);
        int certificationsScore = Math.min(certsCount * 33, 100); // 3+ certifications = 100%

        // 5. Final ATS Score — weighted composite
        int finalScore = (int) (
                (techScore * 0.35) +
                (softScore * 0.10) +
                (experienceScore * 0.20) +
                (educationScore * 0.15) +
                (projectsScore * 0.10) +
                (certificationsScore * 0.10)
        );
        finalScore = Math.max(10, Math.min(finalScore, 100)); // clamp between 10 and 100

        // 6. Assign Grade
        String grade;
        if (finalScore >= 90) grade = "A+";
        else if (finalScore >= 80) grade = "A";
        else if (finalScore >= 70) grade = "B+";
        else if (finalScore >= 60) grade = "B";
        else grade = "C";

        // 7. Generate Suggestions + Examiner Remarks (AI-powered with fallback)
        String suggestionsStr;
        String comments;

        String[] aiFeedback = geminiService.generateExaminerFeedback(
                resume.getRawText(), 
                specialization != null ? specialization.getName() : "Software Engineering",
                matchedTech,
                missingTech,
                finalScore,
                grade
        );

        if (aiFeedback != null) {
            // Use Gemini-generated feedback
            log.info("Using Gemini AI feedback for resume evaluation.");
            comments = aiFeedback[0];
            suggestionsStr = aiFeedback[1];
        } else {
            // Fallback: rule-based STRUCTURED feedback (same JSON shape as Gemini)
            log.info("Gemini unavailable — using rule-based structured feedback.");
            comments = generateStructuredComments(
                    finalScore, grade, specName,
                    matchedTech, missingTech, matchedSoft, missingSoft,
                    experienceScore, educationScore, projectsScore, certsCount
            );
            // Extract changes for DB suggestions column
            List<String> suggestionsList = new ArrayList<>();
            if (!missingTech.isEmpty()) {
                suggestionsList.add("Add these missing technical skills to your resume: " + missingTech.stream().limit(5).collect(Collectors.joining(", ")) + ".");
            }
            if (!missingSoft.isEmpty()) {
                suggestionsList.add("Weave soft skills like " + missingSoft.stream().limit(3).collect(Collectors.joining(", ")) + " into your project descriptions with concrete examples.");
            }
            if (experienceScore < 60) {
                suggestionsList.add("Quantify your accomplishments — add metrics, team sizes, project budgets, and exact tool versions to your work history.");
            }
            if (educationScore < 60) {
                suggestionsList.add("Clearly list your degree name, institution, and graduation year in a dedicated Education section.");
            }
            if (projectsScore < 60) {
                suggestionsList.add("Add 2-3 detailed projects with tech stacks, your role, and measurable outcomes directly related to " + specName + ".");
            }
            if (certsCount == 0) {
                suggestionsList.add("Earn and list at least one relevant certification (AWS, Oracle, Google, Coursera) to strengthen credibility for " + specName + ".");
            }
            suggestionsStr = String.join("##", suggestionsList);
        }

        // Candidate Metadata & Impact Parsing
        String email = extractEmail(resumeText);
        String github = extractGithub(resumeText);
        String candidateName = extractCandidateName(resumeText, email);
        String cgpa = extractCgpa(resumeText);
        int parsedProjectsCount = countProjectsDetail(resumeText);
        Map<String, Object> outcomesMap = evaluateMeasurableOutcomes(resumeText);
        int measurableOutcomesCount = (int) outcomesMap.get("count");
        String measurableOutcomesSummary = (String) outcomesMap.get("summary");

        // Save results
        AtsResult result = new AtsResult();
        result.setResume(resume);
        result.setSpecialization(specialization);
        result.setScore(finalScore);
        result.setGrade(grade);
        result.setMatchingSkills(matchingSkillsStr);
        result.setMissingSkills(missingSkillsStr);
        result.setKeywordCoverage(keywordCoverage * 100);
        result.setExperienceMatch(experienceScore);
        result.setEducationMatch(educationScore);
        result.setProjectsMatch(projectsScore);
        result.setCertificationsCount(certsCount);
        result.setCandidateEmail(email);
        result.setCandidateGithub(github);
        result.setCandidateName(candidateName);
        result.setCgpa(cgpa);
        result.setProjectsCount(parsedProjectsCount);
        result.setMeasurableOutcomesCount(measurableOutcomesCount);
        result.setMeasurableOutcomesSummary(measurableOutcomesSummary);
        result.setSuggestions(suggestionsStr);
        result.setRawComments(comments);

        return result;
    }

    /**
     * Generates structured JSON feedback matching the Gemini output format.
     * Every point references actual evaluation data rather than generic advice.
     */
    private String generateStructuredComments(
            int score, String grade, String specName,
            List<String> matchedTech, List<String> missingTech,
            List<String> matchedSoft, List<String> missingSoft,
            int experienceScore, int educationScore, int projectsScore, int certsCount) {

        // ── Summary ──
        String summary;
        if (score >= 90) {
            summary = "Outstanding resume for " + specName + ". Score: " + score + "/100 (Grade " + grade + "). " +
                    "Strong alignment with " + matchedTech.size() + " technical skills and " + matchedSoft.size() + " soft skills. Highly competitive candidate.";
        } else if (score >= 70) {
            summary = "Solid resume for " + specName + " with score " + score + "/100 (Grade " + grade + "). " +
                    "Good technical foundation with " + matchedTech.size() + " skills matched, but " + missingTech.size() + " key skills are missing. Room for targeted improvement.";
        } else if (score >= 50) {
            summary = "Below-average resume for " + specName + ". Score: " + score + "/100 (Grade " + grade + "). " +
                    "Only " + matchedTech.size() + " of the expected technical skills were found. Significant gaps need addressing before this resume will pass ATS filters.";
        } else {
            summary = "This resume needs major revision for " + specName + ". Score: " + score + "/100 (Grade " + grade + "). " +
                    "Critical skills gaps detected — " + missingTech.size() + " required technical skills are completely absent.";
        }

        // ── Strengths ──
        List<String> strengths = new ArrayList<>();
        if (!matchedTech.isEmpty()) {
            strengths.add("Your resume includes " + matchedTech.size() + " relevant technical skills: " +
                    matchedTech.stream().limit(5).collect(Collectors.joining(", ")) + ". These directly align with " + specName + " requirements.");
        }
        if (!matchedSoft.isEmpty()) {
            strengths.add("Soft skills detected: " + matchedSoft.stream().limit(4).collect(Collectors.joining(", ")) +
                    ". ATS systems increasingly weight interpersonal skills alongside technical abilities.");
        }
        if (experienceScore >= 70) {
            strengths.add("Your experience section scores " + experienceScore + "/100 — the resume demonstrates meaningful work history with relevant role descriptions.");
        }
        if (educationScore >= 80) {
            strengths.add("Education section is strong at " + educationScore + "/100 — academic credentials are clearly presented and well-formatted.");
        }
        if (projectsScore >= 70) {
            strengths.add("Projects section scores " + projectsScore + "/100 — you've included relevant project work that demonstrates hands-on capability.");
        }
        if (certsCount >= 2) {
            strengths.add(certsCount + " certifications detected — this significantly boosts credibility for " + specName + " roles.");
        }
        if (strengths.isEmpty()) {
            strengths.add("Resume is properly formatted and machine-readable by ATS systems.");
        }

        // ── Weaknesses ──
        List<String> weaknesses = new ArrayList<>();
        if (!missingTech.isEmpty()) {
            weaknesses.add(missingTech.size() + " critical technical skills missing for " + specName + ": " +
                    missingTech.stream().limit(5).collect(Collectors.joining(", ")) + ". Most ATS filters will reject resumes lacking these.");
        }
        if (!missingSoft.isEmpty()) {
            weaknesses.add("Missing soft skills: " + missingSoft.stream().limit(3).collect(Collectors.joining(", ")) +
                    ". Many " + specName + " job postings explicitly require these competencies.");
        }
        if (experienceScore < 60) {
            weaknesses.add("Experience section is weak at " + experienceScore + "/100 — work history lacks specific achievements, metrics, or quantified impact.");
        }
        if (educationScore < 60) {
            weaknesses.add("Education section scores only " + educationScore + "/100 — degree details, institution name, or graduation dates may be missing or unclear.");
        }
        if (projectsScore < 60) {
            weaknesses.add("Projects section scores " + projectsScore + "/100 — either no projects are listed, or they lack technical details and outcomes.");
        }
        if (certsCount == 0) {
            weaknesses.add("No certifications detected — in the " + specName + " field, relevant certifications (AWS, Google, Oracle) are a significant differentiator.");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("No major weaknesses identified. Focus on continuous enhancement.");
        }

        // ── What to Change ──
        List<String> changes = new ArrayList<>();
        if (!missingTech.isEmpty()) {
            changes.add("Add these missing skills to your resume: " + missingTech.stream().limit(4).collect(Collectors.joining(", ")) +
                    ". Even listing them in a 'Skills' section will improve ATS match rate.");
        }
        if (experienceScore < 70) {
            changes.add("Rewrite your experience bullets using the X-Y-Z formula: 'Accomplished [X] as measured by [Y] by doing [Z]'. Add specific numbers and tools used.");
        }
        if (projectsScore < 70) {
            changes.add("Add 2-3 projects relevant to " + specName + ". For each, include: project name, tech stack used, your role, and measurable outcome.");
        }
        if (educationScore < 70) {
            changes.add("Format your Education section clearly: Degree Name — Institution Name — Graduation Year. Add relevant coursework if applicable.");
        }
        if (changes.isEmpty()) {
            changes.add("Fine-tune resume formatting for better ATS parsing — use standard section headings like 'Experience', 'Skills', 'Projects', 'Education'.");
        }

        // ── What to Enhance ──
        List<String> enhance = new ArrayList<>();
        if (certsCount < 2) {
            enhance.add("Pursue industry-recognized certifications for " + specName + " — these can increase your ATS score by 10-15 points.");
        }
        if (projectsScore < 85) {
            enhance.add("Host projects on GitHub with README files — recruiters check portfolios, and links in resumes boost both ATS scores and human reviewer interest.");
        }
        if (score < 80) {
            enhance.add("Tailor your resume for each application — mirror the exact keywords from the job description to maximize ATS match percentage.");
        }
        enhance.add("Re-upload your resume after making changes to track your score improvement over time.");

        // ── Build JSON ──
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> feedback = new LinkedHashMap<>();
            feedback.put("summary", summary);
            feedback.put("strengths", strengths);
            feedback.put("weaknesses", weaknesses);
            feedback.put("changes", changes);
            feedback.put("enhance", enhance);
            return mapper.writeValueAsString(feedback);
        } catch (Exception e) {
            log.warning("Failed to serialize structured feedback: " + e.getMessage());
            return summary; // plain text fallback
        }
    }

    private boolean isSkillPresent(String resumeText, String skillName) {
        String skill = skillName.toLowerCase().trim();
        if (skill.isEmpty()) return false;
        
        // If the skill is short and purely alphanumeric (e.g. "go", "r", "aws", "java"), 
        // use word boundaries to avoid false-positive matches (like "go" matching inside "google").
        if (skill.length() <= 4 && skill.matches("^[a-zA-Z0-9]+$")) {
            Pattern p = ACRONYM_PATTERNS.computeIfAbsent(skill, 
                s -> Pattern.compile("\\b" + Pattern.quote(s) + "\\b"));
            return p.matcher(resumeText).find();
        }
        
        return resumeText.contains(skill);
    }

    private List<String> extractKeywordsFromJd(String jd) {
        String[] words = jd.toLowerCase().split("[^a-zA-Z0-9+#\\.]");
        Map<String, Integer> freqMap = new HashMap<>();

        for (String w : words) {
            String trimmed = w.trim();
            if (trimmed.length() > 2 && !STOP_WORDS.contains(trimmed)) {
                freqMap.put(trimmed, freqMap.getOrDefault(trimmed, 0) + 1);
            }
        }

        // Return top 15 words by frequency
        return freqMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int evaluateExperience(String text) {
        int score = 50; // baseline — most resumes mention SOME experience

        // Look for years of experience
        Pattern pattern = Pattern.compile("(\\d+)\\s*(?:-|to)?\\s*(\\d+)?\\s*years?\\b");
        Matcher matcher = pattern.matcher(text);
        int maxYears = 0;
        while (matcher.find()) {
            try {
                int y1 = Integer.parseInt(matcher.group(1));
                maxYears = Math.max(maxYears, y1);
                if (matcher.group(2) != null) {
                    int y2 = Integer.parseInt(matcher.group(2));
                    maxYears = Math.max(maxYears, y2);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (maxYears > 5) score = 100;
        else if (maxYears > 3) score = 90;
        else if (maxYears > 1) score = 75;
        else if (maxYears == 1) score = 65;

        // Boost for experience-related keywords
        if (text.contains("experience") || text.contains("internship") || text.contains("intern ")
                || text.contains("worked as") || text.contains("freelance") || text.contains("contributed")) {
            score = Math.max(score, 65);
        }

        // Boost for action verbs (indicates real work descriptions)
        String[] actionVerbs = {"managed", "developed", "built", "designed", "implemented", "deployed",
                "led", "optimized", "architected", "automated", "configured", "maintained"};
        int verbCount = 0;
        for (String verb : actionVerbs) {
            if (text.contains(verb)) verbCount++;
        }
        if (verbCount >= 4) score = Math.min(100, score + 15);
        else if (verbCount >= 2) score = Math.min(100, score + 10);

        // Boost for senior roles
        if (text.contains("senior") || text.contains("lead") || text.contains("architect")
                || text.contains("manager") || text.contains("principal")) {
            score = Math.min(100, score + 15);
        }


        return score;
    }

    private int evaluateEducation(String text) {
        int score = 30; // baseline
        if (text.contains("bachelor") || text.contains("degree") || text.contains("b.tech") || text.contains("b.e.") || text.contains("b.s.")) {
            score = 80;
        }
        if (text.contains("master") || text.contains("m.tech") || text.contains("m.s.") || text.contains("m.c.a.")) {
            score = 95;
        }
        if (text.contains("ph.d") || text.contains("doctorate")) {
            score = 100;
        }
        if (text.contains("university") || text.contains("college") || text.contains("institute")) {
            score = Math.min(100, score + 10);
        }
        return score;
    }

    private int evaluateProjects(String text) {
        int score = 30;
        if (text.contains("project") || text.contains("projects")) score = 70;
        if (text.contains("github.com") || text.contains("gitlab.com")
                || text.contains("bitbucket.org") || text.contains("portfolio")) score = 85;

        long projectCount = Pattern.compile("\\bproject\\b").matcher(text).results().count();
        if (projectCount >= 3) score = Math.min(100, score + 15);
        return score;
    }

    private int countCertifications(String text) {
        int count = 0;
        String[] certKeywords = {"certified", "certification", "certifications", "credential", "aws", "oracle", "scrum", "cisco", "red hat", "udemy", "coursera"};
        for (String kw : certKeywords) {
            if (text.contains(kw)) {
                count++;
            }
        }
        return count;
    }

    // ── Candidate Detail & Impact Metrics Parsers ──────────────────────────

    private String extractEmail(String text) {
        Pattern p = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(0);
        }
        return "Not specified";
    }

    private String extractGithub(String text) {
        Pattern p = Pattern.compile("(?:https?:\\/\\/)?(?:www\\.)?github\\.com\\/([A-Za-z0-9_-]+)");
        Matcher m = p.matcher(text.toLowerCase());
        if (m.find()) {
            return "github.com/" + m.group(1);
        }
        if (text.toLowerCase().contains("github")) {
            return "GitHub linked";
        }
        return "Not specified";
    }

    private String extractCandidateName(String rawText, String email) {
        if (rawText == null || rawText.isBlank()) return "Candidate";
        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 2 && trimmed.length() < 35 
                    && !trimmed.toLowerCase().contains("resume") 
                    && !trimmed.toLowerCase().contains("curriculum")
                    && !trimmed.contains("@")
                    && !trimmed.matches(".*\\d.*")) {
                return trimmed;
            }
        }
        if (!"Not specified".equals(email)) {
            String prefix = email.split("@")[0];
            return prefix.replace(".", " ").substring(0, 1).toUpperCase() + prefix.replace(".", " ").substring(1);
        }
        return "Candidate Profile";
    }

    private String extractCgpa(String text) {
        String lower = text.toLowerCase();
        
        // Match CGPA formats: 9.2/10, 8.5 cgpa, 3.8 gpa, 8.5/10.0
        Pattern p1 = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\s*(?:\\/|out of)?\\s*(?:10|4)(?:\\.0)?\\s*(?:cgpa|gpa)?\\b");
        Matcher m1 = p1.matcher(lower);
        if (m1.find()) {
            return m1.group(0).toUpperCase();
        }

        Pattern p2 = Pattern.compile("\\b(?:cgpa|gpa|marks|percentage|aggregate)\\s*:?\\s*(\\d+(?:\\.\\d+)?%?)\\b");
        Matcher m2 = p2.matcher(lower);
        if (m2.find()) {
            return m2.group(0).toUpperCase();
        }

        Pattern p3 = Pattern.compile("\\b(\\d{2}\\.?\\d*)%\\b");
        Matcher m3 = p3.matcher(lower);
        if (m3.find()) {
            return m3.group(0);
        }

        return "Not specified";
    }

    private int countProjectsDetail(String text) {
        String lower = text.toLowerCase();
        int count = 0;
        
        // Count explicit occurrences of "project" or project titles/links
        Pattern p = Pattern.compile("\\bproject\\b");
        Matcher m = p.matcher(lower);
        while (m.find()) {
            count++;
        }

        // Count github links or repository links
        Pattern githubP = Pattern.compile("github\\.com\\/[A-Za-z0-9_-]+\\/[A-Za-z0-9_-]+");
        Matcher githubM = githubP.matcher(lower);
        while (githubM.find()) {
            count++;
        }

        if (count == 0 && (lower.contains("developed") || lower.contains("built") || lower.contains("created"))) {
            count = 2; // Baseline estimate if action verbs exist
        }

        return Math.min(count, 10);
    }

    private Map<String, Object> evaluateMeasurableOutcomes(String text) {
        String lower = text.toLowerCase();
        List<String> outcomes = new ArrayList<>();

        // Regex patterns for quantifiable impact
        // 1. Percentage improvements (e.g. 35%, 40% reduction, 150% growth)
        Pattern pctPattern = Pattern.compile("\\b(\\d+%(?:\\s+(?:increase|decrease|improvement|reduction|boost|growth|latency|speed))?)\\b");
        Matcher pctMatcher = pctPattern.matcher(lower);
        while (pctMatcher.find() && outcomes.size() < 4) {
            outcomes.add(pctMatcher.group(0));
        }

        // 2. Scale & Volume (e.g., 50,000+ users, 10M+ records, 100k requests)
        Pattern scalePattern = Pattern.compile("\\b(\\d+[kKmMbB]?\\+?\\s*(?:users|records|requests|downloads|active|rows|views|customers|transactions)?)\\b");
        Matcher scaleMatcher = scalePattern.matcher(lower);
        while (scaleMatcher.find() && outcomes.size() < 6) {
            String val = scaleMatcher.group(0);
            if (val.matches(".*[kKmMbB\\+].*") || val.contains("users") || val.contains("records") || val.contains("requests")) {
                if (!outcomes.contains(val)) outcomes.add(val);
            }
        }

        // 3. Financial savings/budget metrics ($50k, $1M)
        Pattern finPattern = Pattern.compile("\\$\\d+[kKmMbB]?");
        Matcher finMatcher = finPattern.matcher(text);
        while (finMatcher.find() && outcomes.size() < 8) {
            if (!outcomes.contains(finMatcher.group(0))) outcomes.add(finMatcher.group(0));
        }

        int count = outcomes.size();
        String summary;
        if (count >= 4) {
            summary = count + " Impact Metrics Found (High Impact 🌟): " + String.join(", ", outcomes);
        } else if (count >= 2) {
            summary = count + " Impact Metrics Found (Medium Impact): " + String.join(", ", outcomes);
        } else if (count == 1) {
            summary = "1 Impact Metric Found: " + outcomes.get(0);
        } else {
            summary = "Low Impact — No quantifiable metrics (percentages, user scale, latency reductions) detected.";
        }

        return Map.of("count", count, "summary", summary);
    }
}
