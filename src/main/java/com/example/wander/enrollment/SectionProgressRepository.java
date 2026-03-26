package com.example.wander.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionProgressRepository extends JpaRepository<SectionProgressEntity, Long> {

    Optional<SectionProgressEntity> findByUserIdAndCourseIdAndSectionId(Long userId, Long courseId, Long sectionId);

    int deleteByUserIdAndCourseIdAndSectionId(Long userId, Long courseId, Long sectionId);

    List<SectionProgressEntity> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<SectionProgressEntity> findTopByUserIdOrderByCompletedAtDesc(Long userId);

    long deleteByCourseId(Long courseId);

    long countByUserIdAndCourseId(Long userId, Long courseId);

    long deleteByUserId(Long userId);
}
