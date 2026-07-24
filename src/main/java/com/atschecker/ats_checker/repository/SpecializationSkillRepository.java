package com.atschecker.ats_checker.repository;

import com.atschecker.ats_checker.entity.Specialization;
import com.atschecker.ats_checker.entity.SpecializationSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpecializationSkillRepository extends JpaRepository<SpecializationSkill, Long> {
    List<SpecializationSkill> findBySpecialization(Specialization specialization);
    void deleteBySpecialization(Specialization specialization);
}
