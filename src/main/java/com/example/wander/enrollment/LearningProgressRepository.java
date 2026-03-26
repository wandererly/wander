package com.example.wander.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningProgressRepository extends JpaRepository<LearningProgressEntity, Long> {

    Optional<LearningProgressEntity> findByUserIdAndCourseId(Long userId, Long courseId);

    long deleteByCourseId(Long courseId);

    long deleteByUserId(Long userId);
}
