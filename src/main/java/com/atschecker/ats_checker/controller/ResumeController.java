package com.atschecker.ats_checker.controller;

import com.atschecker.ats_checker.entity.AtsResult;
import com.atschecker.ats_checker.entity.Resume;
import com.atschecker.ats_checker.entity.Specialization;
import com.atschecker.ats_checker.entity.User;
import com.atschecker.ats_checker.repository.AtsResultRepository;
import com.atschecker.ats_checker.repository.ResumeRepository;
import com.atschecker.ats_checker.repository.SpecializationRepository;
import com.atschecker.ats_checker.service.AtsEvaluationService;
import com.atschecker.ats_checker.service.ParserService;
import com.atschecker.ats_checker.service.PdfGenerationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ParserService parserService;
    private final AtsEvaluationService evaluationService;
    private final PdfGenerationService pdfGenerationService;
    private final SpecializationRepository specializationRepository;
    private final ResumeRepository resumeRepository;
    private final AtsResultRepository atsResultRepository;

    public ResumeController(ParserService parserService,
                            AtsEvaluationService evaluationService,
                            PdfGenerationService pdfGenerationService,
                            SpecializationRepository specializationRepository,
                            ResumeRepository resumeRepository,
                            AtsResultRepository atsResultRepository) {
        this.parserService             = parserService;
        this.evaluationService         = evaluationService;
        this.pdfGenerationService      = pdfGenerationService;
        this.specializationRepository  = specializationRepository;
        this.resumeRepository          = resumeRepository;
        this.atsResultRepository       = atsResultRepository;
    }

    // ── Shared auth check ──────────────────────────────────────────────────────

    /**
     * Returns true if the current user may access this result
     * (guest result, own result, or admin).
     */
    private boolean canAccess(AtsResult result, User user) {
        User owner = result.getResume().getUser();
        if (owner == null) return true;                          // guest upload
        if (user == null) return false;
        return user.getId().equals(owner.getId()) || "ROLE_ADMIN".equals(user.getRole());
    }

    // ── Endpoints ──────────────────────────────────────────────────────────────

    @GetMapping("/specializations")
    public ResponseEntity<List<Specialization>> getSpecializations() {
        return ResponseEntity.ok(specializationRepository.findAll());
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "specializationId", required = false) Long specializationId,
            @RequestParam(value = "jobDescription",   required = false) String jobDescription,
            HttpSession session) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Uploaded file is empty"));
        }

        try {
            String rawText = parserService.parse(file.getOriginalFilename(), file.getBytes());
            if (rawText == null || rawText.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Could not extract text from resume"));
            }

            User user = (User) session.getAttribute("user");
            Resume resume = resumeRepository.save(new Resume(user, file.getOriginalFilename(), rawText));

            Specialization specialization = specializationId == null ? null
                    : specializationRepository.findById(specializationId).orElse(null);

            AtsResult result = atsResultRepository.save(
                    evaluationService.evaluate(resume, specialization, jobDescription));

            return ResponseEntity.ok(mapAtsResult(result));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error analyzing resume: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in to view history"));
        }
        List<Map<String, Object>> response = atsResultRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapAtsResult)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<?> getDetails(@PathVariable Long id, HttpSession session) {
        AtsResult res = atsResultRepository.findById(id).orElse(null);
        if (res == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Analysis report not found"));
        }
        if (!canAccess(res, (User) session.getAttribute("user"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }
        return ResponseEntity.ok(mapAtsResult(res));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadPdf(@PathVariable Long id, HttpSession session) {
        AtsResult res = atsResultRepository.findById(id).orElse(null);
        if (res == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Analysis report not found");
        }
        if (!canAccess(res, (User) session.getAttribute("user"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            byte[] pdfBytes = pdfGenerationService.generateReportPdf(res);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ATS_Report_" + res.getId() + ".pdf");
            headers.setContentLength(pdfBytes.length);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating PDF: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in to view stats"));
        }

        long   totalChecks = atsResultRepository.countByUser(user);
        Double avgScoreVal = atsResultRepository.getAverageScoreByUser(user);
        int    avgScore    = avgScoreVal != null ? (int) Math.round(avgScoreVal) : 0;

        String mostSelectedRole = "None";
        List<Object[]> roleStats = atsResultRepository.getMostSelectedSpecializationByUser(user);
        if (roleStats != null && !roleStats.isEmpty()) {
            mostSelectedRole = (String) roleStats.get(0)[0];
        }

        List<Map<String, Object>> recentList = atsResultRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream().limit(5)
                .map(this::mapAtsResult)
                .toList();

        return ResponseEntity.ok(Map.of(
                "totalResumesChecked",     totalChecks,
                "averageAtsScore",         avgScore,
                "mostSelectedSpecialization", mostSelectedRole,
                "recentAnalyses",          recentList));
    }

    // ── Mapper ─────────────────────────────────────────────────────────────────

    private Map<String, Object> mapAtsResult(AtsResult res) {
        String raw = res.getSuggestions();
        List<String> suggestions = (raw != null && !raw.isBlank())
                ? Arrays.asList(raw.split("##"))
                : List.of();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",                   res.getId());
        map.put("filename",             res.getResume().getFilename());
        map.put("score",                res.getScore());
        map.put("grade",                res.getGrade());
        map.put("matchingSkills",       res.getMatchingSkills());
        map.put("missingSkills",        res.getMissingSkills());
        map.put("keywordCoverage",      res.getKeywordCoverage());
        map.put("experienceMatch",      res.getExperienceMatch());
        map.put("educationMatch",       res.getEducationMatch());
        map.put("projectsMatch",             res.getProjectsMatch());
        map.put("certificationsCount",       res.getCertificationsCount());
        map.put("candidateEmail",            res.getCandidateEmail() != null ? res.getCandidateEmail() : "Not specified");
        map.put("candidateGithub",           res.getCandidateGithub() != null ? res.getCandidateGithub() : "Not specified");
        map.put("candidateName",             res.getCandidateName() != null ? res.getCandidateName() : "Not specified");
        map.put("cgpa",                      res.getCgpa() != null ? res.getCgpa() : "Not specified");
        map.put("projectsCount",             res.getProjectsCount() != null ? res.getProjectsCount() : 0);
        map.put("measurableOutcomesCount",  res.getMeasurableOutcomesCount() != null ? res.getMeasurableOutcomesCount() : 0);
        map.put("measurableOutcomesSummary", res.getMeasurableOutcomesSummary() != null ? res.getMeasurableOutcomesSummary() : "No measurable outcomes");
        map.put("suggestions",               suggestions);
        map.put("rawComments",               res.getRawComments());
        map.put("createdAt",                 res.getCreatedAt().toString());
        map.put("specializationName",
                res.getSpecialization() != null ? res.getSpecialization().getName() : "Custom / JD only");
        return map;
    }
}
