package com.example.wander.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {

    Optional<EnrollmentEntity> findByUserIdAndCourseId(Long userId, Long courseId);

    List<EnrollmentEntity> findByUserId(Long userId);

    org.springframework.data.domain.Page<EnrollmentEntity> findByUserId(Long userId, org.springframework.data.domain.Pageable pageable);

    long deleteByCourseId(Long courseId);

    long deleteByUserId(Long userId);
}
