package com.example.wander.course;

import com.example.wander.common.BizException;
import com.example.wander.user.UserEntity;
import com.example.wander.user.UserRepository;
import com.example.wander.enrollment.EnrollmentRepository;
import com.example.wander.enrollment.LearningProgressRepository;
import com.example.wander.enrollment.SectionProgressRepository;
import com.example.wander.enrollment.EnrollmentEntity;
import com.example.wander.enrollment.LearningProgressEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final SectionProgressRepository sectionProgressRepository;

    public CourseService(CourseRepository courseRepository,
                         CourseSectionRepository sectionRepository,
                         UserRepository userRepository,
                         EnrollmentRepository enrollmentRepository,
                         LearningProgressRepository learningProgressRepository,
                         SectionProgressRepository sectionProgressRepository) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.learningProgressRepository = learningProgressRepository;
        this.sectionProgressRepository = sectionProgressRepository;
    }

    public Page<CourseEntity> listCourses(int page, int size, String keyword, CourseStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (status == null) {
            if (!hasKeyword) {
                return courseRepository.findAll(pageable);
            }
            return courseRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        }
        if (!hasKeyword) {
            return courseRepository.findByStatus(status, pageable);
        }
        return courseRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, status, pageable);
    }

    public Page<CourseEntity> listTeacherCourses(int page, int size, String keyword, CourseStatus status, String teacherUsername) {
        UserEntity teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new BizException("教师用户不存在"));
        Long teacherId = Objects.requireNonNull(teacher.getId(), "teacher id");
        Pageable pageable = PageRequest.of(page, size);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (status == null) {
            if (!hasKeyword) {
                return courseRepository.findByTeacherId(teacherId, pageable);
            }
            return courseRepository.findByTeacherIdAndTitleContainingIgnoreCase(teacherId, keyword, pageable);
        }
        if (!hasKeyword) {
            return courseRepository.findByTeacherIdAndStatus(teacherId, status, pageable);
        }
        return courseRepository.findByTeacherIdAndTitleContainingIgnoreCaseAndStatus(
                teacherId, keyword, status, pageable);
    }

    @NonNull
    public CourseEntity getById(Long id) {
        Long safeId = Objects.requireNonNull(id, "id");
        return courseRepository.findById(safeId)
                .orElseThrow(() -> new BizException("课程不存在"));
    }

    public List<CourseSectionEntity> listSections(Long courseId) {
        Long safeId = Objects.requireNonNull(courseId, "courseId");
        return sectionRepository.findByCourseIdOrderByOrderIndexAsc(safeId);
    }

    public Page<MyCourseVO> listMyCourses(int page, int size, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("用户不存在"));
        Long userId = Objects.requireNonNull(user.getId(), "user id");
        Pageable pageable = PageRequest.of(page, size);
        Page<EnrollmentEntity> enrollments = enrollmentRepository.findByUserId(userId, pageable);
        List<MyCourseVO> result = new ArrayList<>();
        for (EnrollmentEntity e : enrollments.getContent()) {
            MyCourseVO vo = new MyCourseVO();
            CourseEntity course = Objects.requireNonNull(e.getCourse(), "course");
            Long courseId = Objects.requireNonNull(course.getId(), "course id");
            vo.setCourseId(courseId);
            vo.setTitle(course.getTitle());
            vo.setDescription(course.getDescription());
            vo.setStatus(course.getStatus());
            LearningProgressEntity progress = learningProgressRepository
                    .findByUserIdAndCourseId(userId, courseId)
                    .orElse(null);
            vo.setProgressPercent(progress == null ? 0 : progress.getProgressPercent());
            result.add(vo);
        }
        return new PageImpl<>(result, pageable, enrollments.getTotalElements());
    }

    @Transactional
    public void createCourse(CourseCreateDTO dto, String teacherUsername) {
        UserEntity teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new BizException("教师用户不存在"));

        CourseEntity course = new CourseEntity();
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setStatus(CourseStatus.PUBLISHED);
        course.setTeacher(teacher);
        courseRepository.save(course);
    }

    @Transactional
    public void updateCourse(Long courseId, CourseUpdateDTO dto, String teacherUsername) {
        Long safeId = Objects.requireNonNull(courseId, "courseId");
        CourseEntity course = getById(safeId);
        assertTeacher(course, teacherUsername);

        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new BizException("课程标题不能为空");
            }
            course.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }
        courseRepository.save(course);
    }

    @Transactional
    public void offlineCourse(Long courseId, String teacherUsername) {
        Long safeId = Objects.requireNonNull(courseId, "courseId");
        CourseEntity course = getById(safeId);
        assertTeacher(course, teacherUsername);
        course.setStatus(CourseStatus.OFFLINE);
        courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long courseId, String teacherUsername) {
        Long safeId = Objects.requireNonNull(courseId, "courseId");
        CourseEntity course = getById(safeId);
        assertTeacher(course, teacherUsername);

        sectionProgressRepository.deleteByCourseId(safeId);
        learningProgressRepository.deleteByCourseId(safeId);
        enrollmentRepository.deleteByCourseId(safeId);
        sectionRepository.deleteByCourseId(safeId);
        courseRepository.delete(course);
    }

    @Transactional
    public void createSection(Long courseId, SectionCreateDTO dto, String teacherUsername) {
        Long safeId = Objects.requireNonNull(courseId, "courseId");
        CourseEntity course = getById(safeId);
        assertTeacher(course, teacherUsername);

        CourseSectionEntity section = new CourseSectionEntity();
        section.setCourse(course);
        section.setTitle(dto.getTitle());
        section.setOrderIndex(dto.getOrderIndex());
        section.setContentUrl(dto.getContentUrl());
        sectionRepository.save(section);
    }

    @Transactional
    public void updateSection(Long courseId, Long sectionId, SectionUpdateDTO dto, String teacherUsername) {
        Long safeCourseId = Objects.requireNonNull(courseId, "courseId");
        Long safeSectionId = Objects.requireNonNull(sectionId, "sectionId");
        CourseEntity course = getById(safeCourseId);
        assertTeacher(course, teacherUsername);

        CourseSectionEntity section = sectionRepository.findById(safeSectionId)
                .orElseThrow(() -> new BizException("章节不存在"));
        CourseEntity sectionCourse = Objects.requireNonNull(section.getCourse(), "course");
        Long sectionCourseId = Objects.requireNonNull(sectionCourse.getId(), "course id");
        if (!sectionCourseId.equals(safeCourseId)) {
            throw new BizException("章节不属于该课程");
        }
        section.setTitle(dto.getTitle());
        section.setOrderIndex(dto.getOrderIndex());
        section.setContentUrl(dto.getContentUrl());
        sectionRepository.save(section);
    }

    @Transactional
    public void deleteSection(Long courseId, Long sectionId, String teacherUsername) {
        Long safeCourseId = Objects.requireNonNull(courseId, "courseId");
        Long safeSectionId = Objects.requireNonNull(sectionId, "sectionId");
        CourseEntity course = getById(safeCourseId);
        assertTeacher(course, teacherUsername);

        CourseSectionEntity section = sectionRepository.findById(safeSectionId)
                .orElseThrow(() -> new BizException("章节不存在"));
        CourseEntity sectionCourse = Objects.requireNonNull(section.getCourse(), "course");
        Long sectionCourseId = Objects.requireNonNull(sectionCourse.getId(), "course id");
        if (!sectionCourseId.equals(safeCourseId)) {
            throw new BizException("章节不属于该课程");
        }
        sectionRepository.delete(section);
    }

    private void assertTeacher(CourseEntity course, String teacherUsername) {
        if (!course.getTeacher().getUsername().equals(teacherUsername)) {
            throw new BizException("无权限操作该课程");
        }
    }
}
