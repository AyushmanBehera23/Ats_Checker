package com.atschecker.ats_checker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "specialization_skills")
public class SpecializationSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialization_id", nullable = false)
    private Specialization specialization;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "skill_category", nullable = false)
    private String skillCategory; // "TECHNICAL" or "SOFT"

    public SpecializationSkill() {}

    public SpecializationSkill(Specialization specialization, String skillName, String skillCategory) {
        this.specialization = specialization;
        this.skillName = skillName;
        this.skillCategory = skillCategory;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Specialization getSpecialization() { return specialization; }
    public void setSpecialization(Specialization specialization) { this.specialization = specialization; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getSkillCategory() { return skillCategory; }
    public void setSkillCategory(String skillCategory) { this.skillCategory = skillCategory; }
}
