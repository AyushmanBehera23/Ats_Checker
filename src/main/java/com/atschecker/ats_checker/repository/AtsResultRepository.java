package com.atschecker.ats_checker.repository;

import com.atschecker.ats_checker.entity.AtsResult;
import com.atschecker.ats_checker.entity.Resume;
import com.atschecker.ats_checker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AtsResultRepository extends JpaRepository<AtsResult, Long> {
    Optional<AtsResult> findByResume(Resume resume);
    
    @Query("SELECT a FROM AtsResult a JOIN FETCH a.resume r WHERE r.user = :user ORDER BY a.createdAt DESC")
    List<AtsResult> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT AVG(a.score) FROM AtsResult a JOIN a.resume r WHERE r.user = :user")
    Double getAverageScoreByUser(@Param("user") User user);

    @Query("SELECT COUNT(a) FROM AtsResult a JOIN a.resume r WHERE r.user = :user")
    Long countByUser(@Param("user") User user);

    @Query("SELECT a.specialization.name, COUNT(a) FROM AtsResult a JOIN a.resume r WHERE r.user = :user GROUP BY a.specialization.name ORDER BY COUNT(a) DESC LIMIT 1")
    List<Object[]> getMostSelectedSpecializationByUser(@Param("user") User user);

    @Query("SELECT a.specialization.name, COUNT(a) FROM AtsResult a GROUP BY a.specialization.name ORDER BY COUNT(a) DESC LIMIT 1")
    List<Object[]> getMostSelectedSpecializationAll();
}
