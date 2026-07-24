package com.atschecker.ats_checker.controller;

import com.atschecker.ats_checker.entity.AtsResult;
import com.atschecker.ats_checker.entity.User;
import com.atschecker.ats_checker.repository.AtsResultRepository;
import com.atschecker.ats_checker.service.GeminiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * AiChatController — handles AI chat requests about a specific resume report.
 * Endpoint: POST /api/ai/chat
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final GeminiService geminiService;
    private final AtsResultRepository atsResultRepository;

    public AiChatController(GeminiService geminiService, AtsResultRepository atsResultRepository) {
        this.geminiService = geminiService;
        this.atsResultRepository = atsResultRepository;
    }

    /**
     * Chat with Gemini about a specific resume report.
     * Request body: { "resultId": 5, "message": "What should I improve?" }
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body, HttpSession session) {
        // Optional auth check — allow anonymous chat if result is public
        User user = (User) session.getAttribute("user");

        Object resultIdObj = body.get("resultId");
        String userMessage = (String) body.get("message");

        if (resultIdObj == null || userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "resultId and message are required"));
        }

        long resultId;
        try {
            resultId = Long.parseLong(resultIdObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid resultId"));
        }

        Optional<AtsResult> resultOpt = atsResultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AtsResult result = resultOpt.get();
        String resumeText = result.getResume() != null ? result.getResume().getRawText() : "";
        String specName = result.getSpecialization() != null ? result.getSpecialization().getName() : "Software Engineering";

        if (!geminiService.isAvailable()) {
            return ResponseEntity.ok(Map.of(
                    "reply", "AI chat requires a Gemini API key. Please add gemini.api.key to application.properties and restart the server."
            ));
        }

        String reply = geminiService.chatWithResume(
                resumeText,
                specName,
                result.getScore(),
                result.getGrade(),
                result.getMatchingSkills() != null ? result.getMatchingSkills() : "",
                result.getMissingSkills() != null ? result.getMissingSkills() : "",
                userMessage
        );

        return ResponseEntity.ok(Map.of("reply", reply != null ? reply : "Sorry, I couldn't generate a response right now."));
    }

    /**
     * Check if AI features are available (API key configured).
     */
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "aiEnabled", geminiService.isAvailable(),
                "message", geminiService.isAvailable()
                        ? "Gemini AI is active"
                        : "Add your Gemini API key to application.properties to enable AI features"
        ));
    }
}
