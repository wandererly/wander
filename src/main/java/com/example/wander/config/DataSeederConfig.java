package com.example.wander.config;

import com.example.wander.course.CourseEntity;
import com.example.wander.course.CourseRepository;
import com.example.wander.course.CourseSectionEntity;
import com.example.wander.course.CourseSectionRepository;
import com.example.wander.course.CourseStatus;
import com.example.wander.enrollment.EnrollmentEntity;
import com.example.wander.enrollment.EnrollmentRepository;
import com.example.wander.enrollment.LearningProgressEntity;
import com.example.wander.enrollment.LearningProgressRepository;
import com.example.wander.enrollment.SectionProgressEntity;
import com.example.wander.enrollment.SectionProgressRepository;
import com.example.wander.user.UserEntity;
import com.example.wander.user.UserRepository;
import com.example.wander.user.UserRole;
import com.example.wander.user.UserStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
public class DataSeederConfig {

    @Bean
    public CommandLineRunner seedDemoData(UserRepository userRepository,
                                          CourseRepository courseRepository,
                                          CourseSectionRepository sectionRepository,
                                          EnrollmentRepository enrollmentRepository,
                                          LearningProgressRepository learningProgressRepository,
                                          SectionProgressRepository sectionProgressRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            UserEntity teacher1 = ensureUser(userRepository, passwordEncoder, "teacher1", "teacher1@qq.com",
                    "teacher123", UserRole.TEACHER);
            UserEntity teacher2 = ensureUser(userRepository, passwordEncoder, "teacher2", "teacher2@qq.com",
                    "teacher123", UserRole.TEACHER);
            UserEntity student1 = ensureUser(userRepository, passwordEncoder, "student1", "student1@qq.com",
                    "student123", UserRole.STUDENT);
            UserEntity student2 = ensureUser(userRepository, passwordEncoder, "student2", "student2@qq.com",
                    "student123", UserRole.STUDENT);

            CourseEntity course1 = ensureCourse(courseRepository,
                    "Spring Boot 从零到实战",
                    "快速掌握 Spring Boot 项目搭建、接口开发与安全实践。",
                    CourseStatus.PUBLISHED, teacher1);
            ensureSections(sectionRepository, course1, new String[][]{
                    {"第1章：项目初始化", "https://spring.io/projects/spring-boot"},
                    {"第2章：REST 接口与测试", "https://spring.io/guides"},
                    {"第3章：权限与安全实践", "https://spring.io/guides/topicals/spring-security-architecture"}
            });

            CourseEntity course2 = ensureCourse(courseRepository,
                    "前端工程基础",
                    "涵盖 HTML/CSS/JS 与基础工程化配置。",
                    CourseStatus.PUBLISHED, teacher2);
            ensureSections(sectionRepository, course2, new String[][]{
                    {"第1章：页面结构与语义化", "https://developer.mozilla.org/zh-CN/docs/Web/HTML"},
                    {"第2章：样式布局与响应式", "https://developer.mozilla.org/zh-CN/docs/Web/CSS"},
                    {"第3章：JavaScript 基础", "https://developer.mozilla.org/zh-CN/docs/Web/JavaScript"}
            });

            CourseEntity course3 = ensureCourse(courseRepository,
                    "数据库建模与 SQL 实战",
                    "掌握数据库设计、索引与常用 SQL。",
                    CourseStatus.PUBLISHED, teacher1);
            ensureSections(sectionRepository, course3, new String[][]{
                    {"第1章：关系模型与范式", "https://dev.mysql.com/doc/"},
                    {"第2章：索引与性能优化", "https://dev.mysql.com/doc/"},
                    {"第3章：SQL 查询实战", "https://dev.mysql.com/doc/"}
            });

            CourseEntity course4 = ensureCourse(courseRepository,
                    "Java 面向对象进阶",
                    "面向对象设计、集合与异常处理。",
                    CourseStatus.DRAFT, teacher2);
            ensureSections(sectionRepository, course4, new String[][]{
                    {"第1章：类与对象", ""},
                    {"第2章：集合与泛型", ""},
                    {"第3章：异常与调试", ""}
            });

            ensureEnrollmentWithProgress(enrollmentRepository, learningProgressRepository, sectionProgressRepository,
                    sectionRepository, student1, course1, 1);
            ensureEnrollmentWithProgress(enrollmentRepository, learningProgressRepository, sectionProgressRepository,
                    sectionRepository, student1, course2, 2);
            ensureEnrollmentWithProgress(enrollmentRepository, learningProgressRepository, sectionProgressRepository,
                    sectionRepository, student2, course2, 1);
            ensureEnrollmentWithProgress(enrollmentRepository, learningProgressRepository, sectionProgressRepository,
                    sectionRepository, student2, course3, 2);
        };
    }

    private UserEntity ensureUser(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  String username,
                                  String email,
                                  String rawPassword,
                                  UserRole role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    UserEntity u = new UserEntity();
                    u.setUsername(username);
                    u.setEmail(email);
                    u.setPassword(passwordEncoder.encode(rawPassword));
                    u.setRole(role);
                    u.setStatus(UserStatus.NORMAL);
                    return userRepository.save(u);
                });
    }

    private CourseEntity ensureCourse(CourseRepository courseRepository,
                                      String title,
                                      String description,
                                      CourseStatus status,
                                      UserEntity teacher) {
        Optional<CourseEntity> existing = courseRepository.findAll().stream()
                .filter(c -> c.getTitle() != null && c.getTitle().equals(title))
                .findFirst();
        return existing.orElseGet(() -> {
            CourseEntity c = new CourseEntity();
            c.setTitle(title);
            c.setDescription(description);
            c.setStatus(status);
            c.setTeacher(teacher);
            return courseRepository.save(c);
        });
    }

    private void ensureSections(CourseSectionRepository sectionRepository,
                                CourseEntity course,
                                String[][] sections) {
        if (!sectionRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()).isEmpty()) {
            return;
        }
        int order = 1;
        for (String[] item : sections) {
            CourseSectionEntity section = new CourseSectionEntity();
            section.setCourse(course);
            section.setTitle(item[0]);
            section.setOrderIndex(order++);
            section.setContentUrl(item.length > 1 ? item[1] : "");
            sectionRepository.save(section);
        }
    }

    private void ensureEnrollmentWithProgress(EnrollmentRepository enrollmentRepository,
                                              LearningProgressRepository learningProgressRepository,
                                              SectionProgressRepository sectionProgressRepository,
                                              CourseSectionRepository sectionRepository,
                                              UserEntity student,
                                              CourseEntity course,
                                              int completedCount) {
        EnrollmentEntity enrollment = enrollmentRepository.findByUserIdAndCourseId(student.getId(), course.getId())
                .orElseGet(() -> {
                    EnrollmentEntity e = new EnrollmentEntity();
                    e.setUser(student);
                    e.setCourse(course);
                    return enrollmentRepository.save(e);
                });

        LearningProgressEntity progress = learningProgressRepository
                .findByUserIdAndCourseId(student.getId(), course.getId())
                .orElseGet(() -> {
                    LearningProgressEntity p = new LearningProgressEntity();
                    p.setUser(student);
                    p.setCourse(course);
                    p.setCompletedSectionCount(0);
                    p.setProgressPercent(0);
                    return learningProgressRepository.save(p);
                });

        var sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        int total = sections.size();
        int completed = Math.min(completedCount, total);
        progress.setCompletedSectionCount(completed);
        progress.setProgressPercent(total == 0 ? 0 : (int) Math.round((completed * 100.0) / total));
        learningProgressRepository.save(progress);

        for (int i = 0; i < completed; i++) {
            CourseSectionEntity section = sections.get(i);
            sectionProgressRepository.findByUserIdAndCourseIdAndSectionId(
                    student.getId(), course.getId(), section.getId()
            ).orElseGet(() -> {
                SectionProgressEntity sp = new SectionProgressEntity();
                sp.setUser(student);
                sp.setCourse(course);
                sp.setSection(section);
                return sectionProgressRepository.save(sp);
            });
        }
        if (enrollment.getId() == null) {
            enrollmentRepository.save(enrollment);
        }
    }
}
