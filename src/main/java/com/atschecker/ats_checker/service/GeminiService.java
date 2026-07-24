package com.atschecker.ats_checker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

/**
 * GeminiService — calls Google Gemini 2.0 Flash API via plain Java HttpClient.
 * Requires: gemini.api.key in application.properties
 * All methods have graceful fallback if the key is absent or the call fails.
 */
@Service
public class GeminiService {

    private static final Logger log = Logger.getLogger(GeminiService.class.getName());

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.enabled:true}")
    private boolean enabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate personalized AI examiner remarks and improvement suggestions.
     *
     * @return String array: [0] = examiner remarks, [1] = suggestions (## delimited)
     *         Returns null if Gemini is disabled/unavailable (caller falls back to rule-based).
     */
    public String[] generateExaminerFeedback(
            String resumeText,
            String specializationName,
            List<String> matchedSkills,
            List<String> missingSkills,
            int score,
            String grade) {

        if (!isAvailable()) return null;

        String truncatedResume = resumeText.length() > 4000
                ? resumeText.substring(0, 4000) + "..."
                : resumeText;

        String prompt = String.format("""
            You are a senior ATS resume analyst performing a DEEP, SPECIFIC review.
            You must reference ACTUAL CONTENT from the resume — names, skills, project titles, job titles, dates, technologies — in every single point you make.
            NEVER give generic advice. Every point must be traceable to something in the resume or something missing from it.
            
            CONTEXT:
            - Role applied for: %s
            - ATS Score: %d/100 (Grade: %s)
            - Skills found in resume: %s
            - Skills missing for this role: %s
            
            FULL RESUME TEXT:
            ---
            %s
            ---
            
            Respond with ONLY valid JSON (no markdown, no code fences, no extra text). Use this exact structure:
            {
              "summary": "2-3 sentence overall assessment referencing the candidate's actual name/role and specific observations from the resume",
              "strengths": [
                "Specific strength 1 referencing actual resume content (e.g. 'Your 3 years at XYZ Corp as a Backend Developer shows strong industry exposure')",
                "Specific strength 2",
                "Specific strength 3"
              ],
              "weaknesses": [
                "Specific weakness 1 referencing what's actually wrong or missing (e.g. 'Your Projects section lists only 1 project with no tech stack details')",
                "Specific weakness 2",
                "Specific weakness 3"
              ],
              "changes": [
                "Specific change 1 — exact action to take (e.g. 'Add Docker and Kubernetes to your DevOps project at ABC Inc since you listed Linux skills')",
                "Specific change 2",
                "Specific change 3",
                "Specific change 4"
              ],
              "enhance": [
                "Enhancement 1 — concrete growth opportunity (e.g. 'Get AWS Solutions Architect certification to complement your cloud deployment experience mentioned in Project X')",
                "Enhancement 2",
                "Enhancement 3"
              ]
            }
            
            RULES:
            - Each array must have 3-4 items, never fewer than 3
            - Each item must be 1-2 sentences, specific and actionable
            - Reference actual resume content (project names, company names, skills listed, education details)
            - For missing skills, explain WHY they matter for the %s role specifically
            - Be honest but constructive
            """,
                specializationName, score, grade,
                matchedSkills.isEmpty() ? "None found" : String.join(", ", matchedSkills),
                missingSkills.isEmpty() ? "All matched!" : String.join(", ", missingSkills),
                truncatedResume,
                specializationName
        );

        try {
            String responseText = callGemini(prompt);
            if (responseText == null) return null;

            // Strip any markdown code fences the model might add
            String cleaned = responseText.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\s*", "").replaceAll("```\\s*$", "").trim();
            }

            // Validate it's parseable JSON with expected structure
            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.has("summary")) {
                log.warning("Gemini response missing 'summary' field, falling back.");
                return null;
            }

            // Extract suggestions for DB storage (## delimited for backward compat)
            StringBuilder suggestionsForDb = new StringBuilder();
            JsonNode changes = root.path("changes");
            if (changes.isArray()) {
                for (int i = 0; i < changes.size(); i++) {
                    if (i > 0) suggestionsForDb.append("##");
                    suggestionsForDb.append(changes.get(i).asText());
                }
            }

            // Return: [0] = full JSON string for frontend, [1] = suggestions for DB
            return new String[]{cleaned, suggestionsForDb.toString()};
        } catch (Exception e) {
            log.warning("Gemini structured feedback failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Conversational chat about a specific resume report.
     *
     * @return AI reply string, or null if unavailable.
     */
    public String chatWithResume(
            String resumeText,
            String specializationName,
            int score,
            String grade,
            String matchedSkills,
            String missingSkills,
            String userMessage) {

        if (!isAvailable()) {
            return "AI chat is not available. Please configure your Gemini API key in application.properties.";
        }

        String truncatedResume = resumeText.length() > 2000
                ? resumeText.substring(0, 2000) + "..."
                : resumeText;

        String prompt = String.format("""
            You are an ATS resume coach helping a candidate understand their evaluation results.
            
            CONTEXT:
            - Role: %s
            - ATS Score: %d/100 (Grade: %s)
            - Matched Skills: %s
            - Missing Skills: %s
            - Resume (truncated): %s
            
            CANDIDATE'S QUESTION: %s
            
            Answer concisely (2-4 sentences). Be specific to their resume and score. Be encouraging but honest.
            Do not repeat the context back. Just answer the question directly.
            """,
                specializationName, score, grade,
                matchedSkills, missingSkills,
                truncatedResume,
                userMessage
        );

        try {
            String reply = callGemini(prompt);
            return reply != null ? reply.trim() : "Sorry, I couldn't process your question right now. Please try again.";
        } catch (Exception e) {
            log.warning("Gemini chat failed: " + e.getMessage());
            return "Sorry, the AI assistant encountered an error. Please try again.";
        }
    }

    private String callGemini(String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "contents", java.util.List.of(
                                java.util.Map.of(
                                        "parts", java.util.List.of(
                                                java.util.Map.of("text", prompt)
                                        )
                                )
                        ),
                        "generationConfig", java.util.Map.of(
                                "temperature", 0.7,
                                "maxOutputTokens", 1024
                        )
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warning("Gemini API returned status " + response.statusCode() + ": " + response.body());
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode content = candidates.get(0).path("content").path("parts");
            if (content.isArray() && !content.isEmpty()) {
                return content.get(0).path("text").asText();
            }
        }
        return null;
    }

    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
