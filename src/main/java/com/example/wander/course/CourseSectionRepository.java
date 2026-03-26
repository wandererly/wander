package com.example.wander.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseSectionRepository extends JpaRepository<CourseSectionEntity, Long> {

    List<CourseSectionEntity> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    long deleteByCourseId(Long courseId);
}
