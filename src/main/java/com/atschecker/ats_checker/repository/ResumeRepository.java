package com.atschecker.ats_checker.repository;

import com.atschecker.ats_checker.entity.Resume;
import com.atschecker.ats_checker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserOrderByUploadedAtDesc(User user);
    long countByUser(User user);
}
