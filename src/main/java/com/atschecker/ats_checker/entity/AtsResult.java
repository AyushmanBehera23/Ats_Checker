package com.atschecker.ats_checker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ats_results")
public class AtsResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "specialization_id", nullable = true)
    private Specialization specialization;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private String grade; // "A+", "A", "B+", "B", "C"

    @Lob
    @Column(name = "matching_skills", columnDefinition = "TEXT")
    private String matchingSkills; // Comma-separated or JSON list

    @Lob
    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills; // Comma-separated or JSON list

    @Column(name = "keyword_coverage")
    private Double keywordCoverage;

    @Column(name = "experience_match")
    private Integer experienceMatch; // 0-100 score

    @Column(name = "education_match")
    private Integer educationMatch; // 0-100 score

    @Column(name = "projects_match")
    private Integer projectsMatch; // 0-100 score

    @Column(name = "certifications_count")
    private Integer certificationsCount; // Number of certifications found

    @Column(name = "candidate_email")
    private String candidateEmail;

    @Column(name = "candidate_github")
    private String candidateGithub;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "cgpa")
    private String cgpa;

    @Column(name = "projects_count")
    private Integer projectsCount;

    @Column(name = "measurable_outcomes_count")
    private Integer measurableOutcomesCount;

    @Lob
    @Column(name = "measurable_outcomes_summary", columnDefinition = "TEXT")
    private String measurableOutcomesSummary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String suggestions; // Suggestions JSON or list

    @Lob
    @Column(name = "raw_comments", columnDefinition = "TEXT")
    private String rawComments; // Comments in blue handwritten style

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AtsResult() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public Specialization getSpecialization() { return specialization; }
    public void setSpecialization(Specialization specialization) { this.specialization = specialization; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getMatchingSkills() { return matchingSkills; }
    public void setMatchingSkills(String matchingSkills) { this.matchingSkills = matchingSkills; }

    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }

    public Double getKeywordCoverage() { return keywordCoverage; }
    public void setKeywordCoverage(Double keywordCoverage) { this.keywordCoverage = keywordCoverage; }

    public Integer getExperienceMatch() { return experienceMatch; }
    public void setExperienceMatch(Integer experienceMatch) { this.experienceMatch = experienceMatch; }

    public Integer getEducationMatch() { return educationMatch; }
    public void setEducationMatch(Integer educationMatch) { this.educationMatch = educationMatch; }

    public Integer getProjectsMatch() { return projectsMatch; }
    public void setProjectsMatch(Integer projectsMatch) { this.projectsMatch = projectsMatch; }

    public Integer getCertificationsCount() { return certificationsCount; }
    public void setCertificationsCount(Integer certificationsCount) { this.certificationsCount = certificationsCount; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCandidateGithub() { return candidateGithub; }
    public void setCandidateGithub(String candidateGithub) { this.candidateGithub = candidateGithub; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCgpa() { return cgpa; }
    public void setCgpa(String cgpa) { this.cgpa = cgpa; }

    public Integer getProjectsCount() { return projectsCount; }
    public void setProjectsCount(Integer projectsCount) { this.projectsCount = projectsCount; }

    public Integer getMeasurableOutcomesCount() { return measurableOutcomesCount; }
    public void setMeasurableOutcomesCount(Integer measurableOutcomesCount) { this.measurableOutcomesCount = measurableOutcomesCount; }

    public String getMeasurableOutcomesSummary() { return measurableOutcomesSummary; }
    public void setMeasurableOutcomesSummary(String measurableOutcomesSummary) { this.measurableOutcomesSummary = measurableOutcomesSummary; }

    public String getSuggestions() { return suggestions; }
    public void setSuggestions(String suggestions) { this.suggestions = suggestions; }

    public String getRawComments() { return rawComments; }
    public void setRawComments(String rawComments) { this.rawComments = rawComments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
