package com.example.wander.enrollment;

import com.example.wander.common.ApiResponse;
import com.example.wander.common.BizException;
import com.example.wander.course.CourseEntity;
import com.example.wander.course.CourseService;
import com.example.wander.course.CourseSectionEntity;
import com.example.wander.user.UserEntity;
import com.example.wander.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final LearningProgressRepository progressRepository;
    private final SectionProgressRepository sectionProgressRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;

    public EnrollmentController(EnrollmentRepository enrollmentRepository,
                                LearningProgressRepository progressRepository,
                                SectionProgressRepository sectionProgressRepository,
                                UserRepository userRepository,
                                CourseService courseService) {
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepository = progressRepository;
        this.sectionProgressRepository = sectionProgressRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
    }

    @PostMapping("/{courseId}")
    public ApiResponse<?> enroll(@PathVariable Long courseId,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));
        CourseEntity course = courseService.getById(courseId);

        enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .ifPresent(e -> {
                    throw new BizException("已选过该课程");
                });

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        LearningProgressEntity progress = new LearningProgressEntity();
        progress.setUser(user);
        progress.setCourse(course);
        progress.setCompletedSectionCount(0);
        progress.setProgressPercent(0);
        progressRepository.save(progress);

        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<List<EnrollmentInfoVO>> myCourses(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));
        List<EnrollmentEntity> list = enrollmentRepository.findByUserId(user.getId());
        List<EnrollmentInfoVO> result = list.stream().map(e -> {
            EnrollmentInfoVO vo = new EnrollmentInfoVO();
            vo.setCourseId(e.getCourse().getId());
            vo.setCourseTitle(e.getCourse().getTitle());
            progressRepository.findByUserIdAndCourseId(user.getId(), e.getCourse().getId())
                    .ifPresent(p -> {
                        vo.setProgressPercent(p.getProgressPercent());
                    });
            return vo;
        }).collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    @PostMapping("/{courseId}/progress")
    public ApiResponse<?> updateProgress(@PathVariable Long courseId,
                                         @RequestBody @Valid UpdateProgressDTO dto,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));

        enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("未选该课程"));

        LearningProgressEntity progress = progressRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("学习进度不存在"));

        List<CourseSectionEntity> sections = courseService.listSections(courseId);
        if (sections.isEmpty()) {
            throw new BizException("课程暂无章节");
        }

        int total = sections.size();
        int completed = Math.max(0, Math.min(dto.getCompletedSectionCount(), total));
        int percent = (int) Math.round(completed * 100.0 / total);

        progress.setCompletedSectionCount(completed);
        progress.setProgressPercent(percent);
        progressRepository.save(progress);

        return ApiResponse.ok(null);
    }

    @PostMapping("/{courseId}/sections/complete")
    public ApiResponse<CompleteSectionsResultVO> completeSectionsBatch(@PathVariable Long courseId,
                                                                       @RequestBody @Valid CompleteSectionsDTO dto,
                                                                       @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));

        enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("未选该课程"));

        List<CourseSectionEntity> sections = courseService.listSections(courseId);
        if (sections.isEmpty()) {
            throw new BizException("课程暂无章节");
        }

        java.util.Map<Long, CourseSectionEntity> sectionMap = sections.stream()
                .collect(java.util.stream.Collectors.toMap(CourseSectionEntity::getId, s -> s));

        List<Long> completedIds = new java.util.ArrayList<>();
        List<Long> skippedAlreadyCompleted = new java.util.ArrayList<>();
        List<Long> skippedNotFound = new java.util.ArrayList<>();

        for (Long sectionId : dto.getSectionIds()) {
            CourseSectionEntity section = sectionMap.get(sectionId);
            if (section == null) {
                skippedNotFound.add(sectionId);
                continue;
            }
            boolean alreadyCompleted = sectionProgressRepository
                    .findByUserIdAndCourseIdAndSectionId(user.getId(), courseId, sectionId)
                    .isPresent();
            if (alreadyCompleted) {
                skippedAlreadyCompleted.add(sectionId);
                continue;
            }

            SectionProgressEntity sectionProgress = new SectionProgressEntity();
            sectionProgress.setUser(user);
            sectionProgress.setCourse(section.getCourse());
            sectionProgress.setSection(section);
            sectionProgressRepository.save(sectionProgress);
            completedIds.add(sectionId);
        }

        LearningProgressEntity progress = progressRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("学习进度不存在"));

        long completedCount = sectionProgressRepository.countByUserIdAndCourseId(user.getId(), courseId);
        int total = sections.size();
        int percent = total == 0 ? 0 : (int) Math.round(completedCount * 100.0 / total);
        progress.setCompletedSectionCount((int) completedCount);
        progress.setProgressPercent(percent);
        progressRepository.save(progress);

        CompleteSectionsResultVO result = new CompleteSectionsResultVO();
        result.setCompletedIds(completedIds);
        result.setSkippedAlreadyCompleted(skippedAlreadyCompleted);
        result.setSkippedNotFound(skippedNotFound);
        result.setCompletedSectionCount(progress.getCompletedSectionCount());
        result.setProgressPercent(progress.getProgressPercent());
        return ApiResponse.ok(result);
    }

    @PostMapping("/{courseId}/sections/{sectionId}/complete")
    public ApiResponse<?> completeSection(@PathVariable Long courseId,
                                          @PathVariable Long sectionId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));

        enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("未选该课程"));

        CourseSectionEntity section = courseService.listSections(courseId).stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new BizException("章节不存在"));

        sectionProgressRepository.findByUserIdAndCourseIdAndSectionId(user.getId(), courseId, sectionId)
                .ifPresent(p -> {
                    throw new BizException("章节已完成");
                });

        SectionProgressEntity sectionProgress = new SectionProgressEntity();
        sectionProgress.setUser(user);
        sectionProgress.setCourse(section.getCourse());
        sectionProgress.setSection(section);
        sectionProgressRepository.save(sectionProgress);

        LearningProgressEntity progress = progressRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("学习进度不存在"));

        long completedCount = sectionProgressRepository.countByUserIdAndCourseId(user.getId(), courseId);
        List<CourseSectionEntity> sections = courseService.listSections(courseId);
        int total = sections.size();
        int percent = total == 0 ? 0 : (int) Math.round(completedCount * 100.0 / total);
        progress.setCompletedSectionCount((int) completedCount);
        progress.setProgressPercent(percent);
        progressRepository.save(progress);

        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{courseId}/sections/{sectionId}/complete")
    public ApiResponse<?> undoCompleteSection(@PathVariable Long courseId,
                                              @PathVariable Long sectionId,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));

        enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("未选该课程"));

        SectionProgressEntity progressEntity = Objects.requireNonNull(
                sectionProgressRepository.findByUserIdAndCourseIdAndSectionId(user.getId(), courseId, sectionId)
                        .orElseThrow(() -> new BizException("章节未完成")),
                "section progress"
        );
        sectionProgressRepository.delete(progressEntity);

        LearningProgressEntity progress = progressRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("学习进度不存在"));

        long completedCount = sectionProgressRepository.countByUserIdAndCourseId(user.getId(), courseId);
        List<CourseSectionEntity> sections = courseService.listSections(courseId);
        int total = sections.size();
        int percent = total == 0 ? 0 : (int) Math.round(completedCount * 100.0 / total);
        progress.setCompletedSectionCount((int) completedCount);
        progress.setProgressPercent(percent);
        progressRepository.save(progress);

        return ApiResponse.ok(null);
    }

    @GetMapping("/{courseId}/progress")
    public ApiResponse<ProgressDetailVO> progressDetail(@PathVariable Long courseId,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));

        enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("未选该课程"));

        LearningProgressEntity progress = progressRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BizException("学习进度不存在"));

        List<CourseSectionEntity> sections = courseService.listSections(courseId);
        Set<Long> completedIds = sectionProgressRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .stream()
                .map(sp -> sp.getSection().getId())
                .collect(Collectors.toSet());

        List<SectionProgressVO> sectionProgress = sections.stream().map(s -> {
            SectionProgressVO vo = new SectionProgressVO();
            vo.setSectionId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setOrderIndex(s.getOrderIndex());
            vo.setCompleted(completedIds.contains(s.getId()));
            return vo;
        }).collect(Collectors.toList());

        ProgressDetailVO vo = new ProgressDetailVO();
        vo.setCourseId(courseId);
        vo.setCompletedSectionCount(progress.getCompletedSectionCount());
        vo.setProgressPercent(progress.getProgressPercent());
        vo.setSections(sectionProgress);
        return ApiResponse.ok(vo);
    }

    public static class UpdateProgressDTO {
        @NotNull
        private Integer completedSectionCount;

        public Integer getCompletedSectionCount() {
            return completedSectionCount;
        }

        public void setCompletedSectionCount(Integer completedSectionCount) {
            this.completedSectionCount = completedSectionCount;
        }
    }

    public static class CompleteSectionsDTO {
        @NotNull
        private List<Long> sectionIds;

        public List<Long> getSectionIds() {
            return sectionIds;
        }

        public void setSectionIds(List<Long> sectionIds) {
            this.sectionIds = sectionIds;
        }
    }

    public static class CompleteSectionsResultVO {
        private List<Long> completedIds;
        private List<Long> skippedAlreadyCompleted;
        private List<Long> skippedNotFound;
        private Integer completedSectionCount;
        private Integer progressPercent;

        public List<Long> getCompletedIds() {
            return completedIds;
        }

        public void setCompletedIds(List<Long> completedIds) {
            this.completedIds = completedIds;
        }

        public List<Long> getSkippedAlreadyCompleted() {
            return skippedAlreadyCompleted;
        }

        public void setSkippedAlreadyCompleted(List<Long> skippedAlreadyCompleted) {
            this.skippedAlreadyCompleted = skippedAlreadyCompleted;
        }

        public List<Long> getSkippedNotFound() {
            return skippedNotFound;
        }

        public void setSkippedNotFound(List<Long> skippedNotFound) {
            this.skippedNotFound = skippedNotFound;
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

    public static class ProgressDetailVO {
        private Long courseId;
        private Integer completedSectionCount;
        private Integer progressPercent;
        private List<SectionProgressVO> sections;

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
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

        public List<SectionProgressVO> getSections() {
            return sections;
        }

        public void setSections(List<SectionProgressVO> sections) {
            this.sections = sections;
        }
    }

    public static class SectionProgressVO {
        private Long sectionId;
        private String title;
        private Integer orderIndex;
        private boolean completed;

        public Long getSectionId() {
            return sectionId;
        }

        public void setSectionId(Long sectionId) {
            this.sectionId = sectionId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

    public static class EnrollmentInfoVO {
        private Long courseId;
        private String courseTitle;
        private Integer progressPercent;

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }

        public String getCourseTitle() {
            return courseTitle;
        }

        public void setCourseTitle(String courseTitle) {
            this.courseTitle = courseTitle;
        }

        public Integer getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(Integer progressPercent) {
            this.progressPercent = progressPercent;
        }
    }
}
