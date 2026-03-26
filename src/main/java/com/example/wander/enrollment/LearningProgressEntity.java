package com.example.wander.enrollment;

import com.example.wander.course.CourseEntity;
import com.example.wander.user.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "learning_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class LearningProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    private Integer completedSectionCount;

    private Integer progressPercent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public CourseEntity getCourse() {
        return course;
    }

    public void setCourse(CourseEntity course) {
        this.course = course;
    }

    public Integer getCompletedSectionCount() {
        return completedSectionCount;
    }

    public void setCompletedSectionCount(Integer completedSectionCount) {
        this.completedSectionCount = completedSectionCount;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }
}

