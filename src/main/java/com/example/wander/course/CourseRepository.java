package com.example.wander.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    Page<CourseEntity> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<CourseEntity> findByStatus(CourseStatus status, Pageable pageable);

    Page<CourseEntity> findByTitleContainingIgnoreCaseAndStatus(String keyword, CourseStatus status, Pageable pageable);

    Page<CourseEntity> findByTeacherId(Long teacherId, Pageable pageable);

    Page<CourseEntity> findByTeacherIdAndTitleContainingIgnoreCase(Long teacherId, String keyword, Pageable pageable);

    Page<CourseEntity> findByTeacherIdAndStatus(Long teacherId, CourseStatus status, Pageable pageable);

    Page<CourseEntity> findByTeacherIdAndTitleContainingIgnoreCaseAndStatus(
            Long teacherId, String keyword, CourseStatus status, Pageable pageable);

    java.util.List<CourseEntity> findByTeacherId(Long teacherId);
}
