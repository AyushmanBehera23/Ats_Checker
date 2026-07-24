package com.atschecker.ats_checker.controller;

import com.atschecker.ats_checker.entity.Specialization;
import com.atschecker.ats_checker.entity.SpecializationSkill;
import com.atschecker.ats_checker.entity.User;
import com.atschecker.ats_checker.repository.SpecializationRepository;
import com.atschecker.ats_checker.repository.SpecializationSkillRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SpecializationRepository specializationRepository;
    private final SpecializationSkillRepository skillRepository;

    public AdminController(SpecializationRepository specializationRepository,
                           SpecializationSkillRepository skillRepository) {
        this.specializationRepository = specializationRepository;
        this.skillRepository = skillRepository;
    }

    // ── Guard ──────────────────────────────────────────────────────────────────

    /** Returns a FORBIDDEN response, or null if the caller IS an admin. */
    private ResponseEntity<?> requireAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied. Admin role required."));
        }
        return null;
    }

    // ── Specializations ────────────────────────────────────────────────────────

    @PostMapping("/specializations")
    public ResponseEntity<?> addSpecialization(@RequestBody Map<String, String> body, HttpSession session) {
        ResponseEntity<?> guard = requireAdmin(session);
        if (guard != null) return guard;

        String name        = body.get("name");
        String description = body.get("description");

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Specialization name is required"));
        }
        if (specializationRepository.findByName(name).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Specialization already exists"));
        }

        Specialization spec = specializationRepository.save(new Specialization(name, description));
        return ResponseEntity.status(HttpStatus.CREATED).body(spec);
    }

    @DeleteMapping("/specializations/{id}")
    public ResponseEntity<?> deleteSpecialization(@PathVariable Long id, HttpSession session) {
        ResponseEntity<?> guard = requireAdmin(session);
        if (guard != null) return guard;

        Specialization spec = specializationRepository.findById(id).orElse(null);
        if (spec == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Specialization not found"));
        }

        specializationRepository.delete(spec);
        return ResponseEntity.ok(Map.of("message", "Specialization deleted successfully"));
    }

    // ── Skills ─────────────────────────────────────────────────────────────────

    @GetMapping("/skills/{specId}")
    public ResponseEntity<?> getSkillsForSpec(@PathVariable Long specId) {
        Specialization spec = specializationRepository.findById(specId).orElse(null);
        if (spec == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Specialization not found"));
        }
        List<SpecializationSkill> skills = skillRepository.findBySpecialization(spec);
        return ResponseEntity.ok(skills);
    }

    @PostMapping("/skills")
    public ResponseEntity<?> addSkill(@RequestBody Map<String, Object> body, HttpSession session) {
        ResponseEntity<?> guard = requireAdmin(session);
        if (guard != null) return guard;

        Object specIdRaw = body.get("specializationId");
        Object nameRaw   = body.get("name");
        Object catRaw    = body.get("category");

        if (specIdRaw == null || nameRaw == null || catRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "specializationId, name, and category are required"));
        }

        Long   specId   = Long.valueOf(specIdRaw.toString());
        String name     = nameRaw.toString().trim();
        String category = catRaw.toString().toUpperCase();

        Specialization spec = specializationRepository.findById(specId).orElse(null);
        if (spec == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Specialization not found"));
        }

        SpecializationSkill skill = skillRepository.save(new SpecializationSkill(spec, name, category));
        return ResponseEntity.status(HttpStatus.CREATED).body(skill);
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<?> deleteSkill(@PathVariable Long id, HttpSession session) {
        ResponseEntity<?> guard = requireAdmin(session);
        if (guard != null) return guard;

        SpecializationSkill skill = skillRepository.findById(id).orElse(null);
        if (skill == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Skill not found"));
        }

        skillRepository.delete(skill);
        return ResponseEntity.ok(Map.of("message", "Skill deleted successfully"));
    }
}
