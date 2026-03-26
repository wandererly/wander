package com.example.wander.course;

import com.example.wander.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ApiResponse<Page<CourseEntity>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CourseStatus status
    ) {
        return ApiResponse.ok(courseService.listCourses(page, size, keyword, status));
    }

    @GetMapping("/my")
    public ApiResponse<Page<MyCourseVO>> myCourses(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(courseService.listMyCourses(page, size, userDetails.getUsername()));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/my-teaching")
    public ApiResponse<Page<CourseEntity>> myTeaching(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) CourseStatus status,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(courseService.listTeacherCourses(page, size, keyword, status, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseEntity> detail(@PathVariable Long id) {
        return ApiResponse.ok(courseService.getById(id));
    }

    @GetMapping("/{id}/sections")
    public ApiResponse<List<CourseSectionEntity>> sections(@PathVariable Long id) {
        return ApiResponse.ok(courseService.listSections(id));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public ApiResponse<?> create(@RequestBody @Valid CourseCreateDTO dto,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        courseService.createCourse(dto, userDetails.getUsername());
        return ApiResponse.ok(null);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id,
                                 @RequestBody CourseUpdateDTO dto,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        courseService.updateCourse(id, dto, userDetails.getUsername());
        return ApiResponse.ok(null);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/{id}/offline")
    public ApiResponse<?> offline(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        courseService.offlineCourse(id, userDetails.getUsername());
        return ApiResponse.ok(null);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        courseService.deleteCourse(id, userDetails.getUsername());
        return ApiResponse.ok(null);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{id}/sections")
    public ApiResponse<?> createSection(@PathVariable Long id,
                                        @RequestBody @Valid SectionCreateDTO dto,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        courseService.createSection(id, dto, userDetails.getUsername());
        return ApiResponse.ok(null);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}/sections/{sectionId}")
    public ApiResponse<?> updateSection(@PathVariable Long id,
                                        @PathVariable Long sectionId,
                                        @RequestBody @Valid SectionUpdateDTO dto,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        courseService.updateSection(id, sectionId, dto, userDetails.getUsername());
        return ApiResponse.ok(null);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}/sections/{sectionId}")
    public ApiResponse<?> deleteSection(@PathVariable Long id,
                                        @PathVariable Long sectionId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        courseService.deleteSection(id, sectionId, userDetails.getUsername());
        return ApiResponse.ok(null);
    }
}
