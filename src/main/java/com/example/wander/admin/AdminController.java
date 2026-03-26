package com.example.wander.admin;

import com.example.wander.common.ApiResponse;
import com.example.wander.common.BizException;
import com.example.wander.user.RoleChangeRequestEntity;
import com.example.wander.user.RoleChangeRequestRepository;
import com.example.wander.user.RoleChangeStatus;
import com.example.wander.user.UserEntity;
import com.example.wander.user.UserRepository;
import com.example.wander.user.UserRole;
import com.example.wander.user.UserStatus;
import com.example.wander.notification.NotificationEntity;
import com.example.wander.notification.NotificationRepository;
import com.example.wander.enrollment.EnrollmentEntity;
import com.example.wander.enrollment.EnrollmentRepository;
import com.example.wander.enrollment.LearningProgressRepository;
import com.example.wander.enrollment.LearningProgressEntity;
import com.example.wander.enrollment.SectionProgressRepository;
import com.example.wander.course.CourseRepository;
import com.example.wander.course.CourseSectionRepository;
import com.example.wander.course.CourseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleChangeRequestRepository roleChangeRequestRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final AuditLogRepository auditLogRepository;
    private final SectionProgressRepository sectionProgressRepository;
    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;

    public AdminController(UserRepository userRepository,
                           RoleChangeRequestRepository roleChangeRequestRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder,
                           EnrollmentRepository enrollmentRepository,
                           LearningProgressRepository learningProgressRepository,
                           AuditLogRepository auditLogRepository,
                           SectionProgressRepository sectionProgressRepository,
                           CourseRepository courseRepository,
                           CourseSectionRepository courseSectionRepository) {
        this.userRepository = userRepository;
        this.roleChangeRequestRepository = roleChangeRequestRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.enrollmentRepository = enrollmentRepository;
        this.learningProgressRepository = learningProgressRepository;
        this.auditLogRepository = auditLogRepository;
        this.sectionProgressRepository = sectionProgressRepository;
        this.courseRepository = courseRepository;
        this.courseSectionRepository = courseSectionRepository;
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<?> updateRole(@PathVariable Long id,
                                     @RequestBody @Valid UpdateRoleDTO dto,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long safeId = Objects.requireNonNull(id, "id");
        UserEntity user = userRepository.findById(safeId)
                .orElseThrow(() -> new BizException("用户不存在"));
        user.setRole(dto.getRole());
        userRepository.save(user);
        logAction(userDetails, "UPDATE_ROLE", "USER", safeId, "role=" + dto.getRole());
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<Page<UserInfoVO>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status) {
        Specification<UserEntity> spec = buildUserSpec(keyword, role, status);
        Page<UserInfoVO> result = userRepository.findAll(spec, PageRequest.of(page, size))
                .map(u -> {
                    UserInfoVO vo = new UserInfoVO();
                    vo.setId(Objects.requireNonNull(u.getId(), "user id"));
                    vo.setUsername(u.getUsername());
                    vo.setEmail(u.getEmail());
                    vo.setRole(u.getRole());
                    vo.setStatus(u.getStatus());
                    return vo;
                });
        return ApiResponse.ok(result);
    }

    @GetMapping("/role-requests")
    public ApiResponse<List<RoleRequestVO>> listRoleRequests(
            @RequestParam(required = false) RoleChangeStatus status) {
        List<RoleChangeRequestEntity> entities = (status == null)
                ? roleChangeRequestRepository.findAll()
                : roleChangeRequestRepository.findByStatus(status);
        List<RoleRequestVO> list = entities.stream().map(r -> {
            RoleRequestVO vo = new RoleRequestVO();
            vo.setId(Objects.requireNonNull(r.getId(), "request id"));
            UserEntity user = Objects.requireNonNull(r.getUser(), "user");
            vo.setUserId(Objects.requireNonNull(user.getId(), "user id"));
            vo.setUsername(user.getUsername());
            vo.setCurrentRole(user.getRole());
            vo.setTargetRole(r.getTargetRole());
            vo.setStatus(r.getStatus());
            vo.setReason(r.getReason());
            vo.setAdminNote(r.getAdminNote());
            vo.setNotified(r.isNotified());
            return vo;
        }).collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    @PatchMapping("/role-requests/{id}/approve")
    public ApiResponse<?> approveRequest(@PathVariable Long id,
                                         @RequestBody(required = false) AdminNoteDTO dto,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        Long safeId = Objects.requireNonNull(id, "id");
        RoleChangeRequestEntity req = roleChangeRequestRepository.findById(safeId)
                .orElseThrow(() -> new BizException("申请不存在"));
        Objects.requireNonNull(req.getId(), "request id");
        if (req.getStatus() != RoleChangeStatus.PENDING) {
            throw new BizException("申请已处理");
        }
        UserEntity user = req.getUser();
        user.setRole(req.getTargetRole());
        userRepository.save(user);
        req.setStatus(RoleChangeStatus.APPROVED);
        if (dto != null) {
            req.setAdminNote(dto.getAdminNote());
        }
        req.setNotified(true);
        roleChangeRequestRepository.save(req);
        NotificationEntity notification = new NotificationEntity();
        notification.setUser(user);
        notification.setTitle("角色申请已通过");
        String note = (dto == null || dto.getAdminNote() == null || dto.getAdminNote().isBlank())
                ? "管理员已通过你的角色申请"
                : "管理员备注: " + dto.getAdminNote();
        notification.setContent(note);
        notificationRepository.save(notification);
        logAction(userDetails, "APPROVE_ROLE_REQUEST", "ROLE_REQUEST", safeId, "to=" + req.getTargetRole());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/role-requests/{id}/reject")
    public ApiResponse<?> rejectRequest(@PathVariable Long id,
                                        @RequestBody(required = false) AdminNoteDTO dto,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        Long safeId = Objects.requireNonNull(id, "id");
        RoleChangeRequestEntity req = roleChangeRequestRepository.findById(safeId)
                .orElseThrow(() -> new BizException("申请不存在"));
        Objects.requireNonNull(req.getId(), "request id");
        if (req.getStatus() != RoleChangeStatus.PENDING) {
            throw new BizException("申请已处理");
        }
        req.setStatus(RoleChangeStatus.REJECTED);
        if (dto != null) {
            req.setAdminNote(dto.getAdminNote());
        }
        req.setNotified(true);
        roleChangeRequestRepository.save(req);
        UserEntity user = req.getUser();
        NotificationEntity notification = new NotificationEntity();
        notification.setUser(user);
        notification.setTitle("角色申请被拒绝");
        String note = (dto == null || dto.getAdminNote() == null || dto.getAdminNote().isBlank())
                ? "管理员已拒绝你的角色申请"
                : "管理员备注: " + dto.getAdminNote();
        notification.setContent(note);
        notificationRepository.save(notification);
        logAction(userDetails, "REJECT_ROLE_REQUEST", "ROLE_REQUEST", safeId, "reason=" + (dto == null ? "" : dto.getAdminNote()));
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateStatus(@PathVariable Long id,
                                       @RequestBody @Valid UpdateStatusDTO dto,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        Long safeId = Objects.requireNonNull(id, "id");
        UserEntity user = userRepository.findById(safeId)
                .orElseThrow(() -> new BizException("用户不存在"));
        user.setStatus(dto.getStatus());
        userRepository.save(user);
        logAction(userDetails, "UPDATE_STATUS", "USER", safeId, "status=" + dto.getStatus());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/password")
    public ApiResponse<?> resetPassword(@PathVariable Long id,
                                        @RequestBody @Valid ResetPasswordDTO dto,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        Long safeId = Objects.requireNonNull(id, "id");
        UserEntity user = userRepository.findById(safeId)
                .orElseThrow(() -> new BizException("用户不存在"));
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
        logAction(userDetails, "RESET_PASSWORD", "USER", safeId, "manual_reset");
        return ApiResponse.ok(null);
    }

    @PostMapping
    public ApiResponse<?> createUser(@RequestBody @Valid CreateUserDTO dto,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            return ApiResponse.fail("用户名已存在");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            return ApiResponse.fail("邮箱已存在");
        }
        UserEntity user = new UserEntity();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus());
        userRepository.save(user);
        logAction(userDetails, "CREATE_USER", "USER", user.getId(), "role=" + dto.getRole());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteUser(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long safeId = Objects.requireNonNull(id, "id");
        UserEntity user = userRepository.findById(safeId)
                .orElseThrow(() -> new BizException("用户不存在"));

        // 删除教师课程及关联数据
        List<CourseEntity> teaching = courseRepository.findByTeacherId(safeId);
        for (CourseEntity course : teaching) {
            Long courseId = course.getId();
            if (courseId != null) {
                sectionProgressRepository.deleteByCourseId(courseId);
                learningProgressRepository.deleteByCourseId(courseId);
                enrollmentRepository.deleteByCourseId(courseId);
                courseSectionRepository.deleteByCourseId(courseId);
                courseRepository.delete(course);
            }
        }

        // 删除用户关联数据
        sectionProgressRepository.deleteByUserId(safeId);
        learningProgressRepository.deleteByUserId(safeId);
        enrollmentRepository.deleteByUserId(safeId);
        roleChangeRequestRepository.deleteByUserId(safeId);
        notificationRepository.deleteByUserId(safeId);

        userRepository.delete(user);
        logAction(userDetails, "DELETE_USER", "USER", safeId, "cascade=true");
        return ApiResponse.ok(null);
    }

    @PostMapping("/batch/role")
    public ApiResponse<?> batchUpdateRole(@RequestBody @Valid BatchRoleDTO dto,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        for (Long userId : dto.getUserIds()) {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new BizException("用户不存在: " + userId));
            user.setRole(dto.getRole());
            userRepository.save(user);
        }
        logAction(userDetails, "BATCH_UPDATE_ROLE", "USER", null, "role=" + dto.getRole() + ",count=" + dto.getUserIds().size());
        return ApiResponse.ok(null);
    }

    @PostMapping("/batch/status")
    public ApiResponse<?> batchUpdateStatus(@RequestBody @Valid BatchStatusDTO dto,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        for (Long userId : dto.getUserIds()) {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new BizException("用户不存在: " + userId));
            user.setStatus(dto.getStatus());
            userRepository.save(user);
        }
        logAction(userDetails, "BATCH_UPDATE_STATUS", "USER", null, "status=" + dto.getStatus() + ",count=" + dto.getUserIds().size());
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<UserDetailVO> userDetail(@PathVariable Long id) {
        Long safeId = Objects.requireNonNull(id, "id");
        UserEntity user = userRepository.findById(safeId)
                .orElseThrow(() -> new BizException("用户不存在"));
        UserDetailVO detail = new UserDetailVO();
        detail.setId(Objects.requireNonNull(user.getId(), "user id"));
        detail.setUsername(user.getUsername());
        detail.setEmail(user.getEmail());
        detail.setRole(user.getRole());
        detail.setStatus(user.getStatus());
        detail.setCreatedAt(user.getCreatedAt());
        detail.setUpdatedAt(user.getUpdatedAt());

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByUserId(safeId);
        List<UserCourseVO> courses = new ArrayList<>();
        for (EnrollmentEntity e : enrollments) {
            UserCourseVO vo = new UserCourseVO();
            CourseEntity course = Objects.requireNonNull(e.getCourse(), "course");
            Long courseId = Objects.requireNonNull(course.getId(), "course id");
            vo.setCourseId(courseId);
            vo.setTitle(course.getTitle());
            LearningProgressEntity progress = learningProgressRepository
                    .findByUserIdAndCourseId(safeId, courseId)
                    .orElse(null);
            vo.setProgressPercent(progress == null ? 0 : progress.getProgressPercent());
            courses.add(vo);
        }
        detail.setCourses(courses);

        sectionProgressRepository.findTopByUserIdOrderByCompletedAtDesc(safeId)
                .ifPresent(sp -> detail.setLastLearningAt(sp.getCompletedAt()));
        return ApiResponse.ok(detail);
    }

    @GetMapping("/audit-logs")
    public ApiResponse<Page<AuditLogVO>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType) {
        Specification<AuditLogEntity> spec = buildAuditSpec(keyword, action, targetType);
        Page<AuditLogVO> result = auditLogRepository.findAll(spec, PageRequest.of(page, size))
                .map(a -> {
                    AuditLogVO vo = new AuditLogVO();
                    vo.setId(Objects.requireNonNull(a.getId(), "audit id"));
                    vo.setAction(a.getAction());
                    vo.setTargetType(a.getTargetType());
                    vo.setTargetId(a.getTargetId());
                    vo.setDetail(a.getDetail());
                    vo.setCreatedAt(a.getCreatedAt());
                    UserEntity admin = Objects.requireNonNull(a.getAdminUser(), "admin user");
                    vo.setAdminUsername(admin.getUsername());
                    return vo;
                });
        return ApiResponse.ok(result);
    }

    @GetMapping(value = "/audit-logs/export", produces = "text/csv")
    public void exportAuditLogs(@RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String action,
                                @RequestParam(required = false) String targetType,
                                HttpServletResponse response) throws java.io.IOException {
        Specification<AuditLogEntity> spec = buildAuditSpec(keyword, action, targetType);
        List<AuditLogEntity> logs = auditLogRepository.findAll(spec);
        response.setHeader("Content-Disposition", "attachment; filename=\"audit-logs.csv\"");
        response.setContentType("text/csv; charset=UTF-8");
        java.io.PrintWriter writer = response.getWriter();
        writer.println("id,action,targetType,targetId,detail,adminUsername,createdAt");
        for (AuditLogEntity log : logs) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                    safeCsv(log.getId()),
                    safeCsv(log.getAction()),
                    safeCsv(log.getTargetType()),
                    safeCsv(log.getTargetId()),
                    safeCsv(log.getDetail()),
                    safeCsv(log.getAdminUser() == null ? null : log.getAdminUser().getUsername()),
                    safeCsv(log.getCreatedAt()));
        }
        writer.flush();
    }

    public static class UpdateRoleDTO {
        @NotNull
        private UserRole role;

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }
    }

    public static class UpdateStatusDTO {
        @NotNull
        private UserStatus status;

        public UserStatus getStatus() {
            return status;
        }

        public void setStatus(UserStatus status) {
            this.status = status;
        }
    }

    public static class ResetPasswordDTO {
        @NotNull
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class CreateUserDTO {
        @NotNull
        private String username;
        @NotNull
        private String email;
        @NotNull
        private String password;
        @NotNull
        private UserRole role;
        @NotNull
        private UserStatus status;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }

        public UserStatus getStatus() {
            return status;
        }

        public void setStatus(UserStatus status) {
            this.status = status;
        }
    }

    public static class UserInfoVO {
        private Long id;
        private String username;
        private String email;
        private UserRole role;
        private UserStatus status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }

        public UserStatus getStatus() {
            return status;
        }

        public void setStatus(UserStatus status) {
            this.status = status;
        }
    }

    public static class RoleRequestVO {
        private Long id;
        private Long userId;
        private String username;
        private UserRole currentRole;
        private UserRole targetRole;
        private RoleChangeStatus status;
        private String reason;
        private String adminNote;
        private boolean notified;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public UserRole getCurrentRole() {
            return currentRole;
        }

        public void setCurrentRole(UserRole currentRole) {
            this.currentRole = currentRole;
        }

        public UserRole getTargetRole() {
            return targetRole;
        }

        public void setTargetRole(UserRole targetRole) {
            this.targetRole = targetRole;
        }

        public RoleChangeStatus getStatus() {
            return status;
        }

        public void setStatus(RoleChangeStatus status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getAdminNote() {
            return adminNote;
        }

        public void setAdminNote(String adminNote) {
            this.adminNote = adminNote;
        }

        public boolean isNotified() {
            return notified;
        }

        public void setNotified(boolean notified) {
            this.notified = notified;
        }
    }

    public static class AdminNoteDTO {
        private String adminNote;

        public String getAdminNote() {
            return adminNote;
        }

        public void setAdminNote(String adminNote) {
            this.adminNote = adminNote;
        }
    }

    public static class BatchRoleDTO {
        @NotNull
        private List<Long> userIds;
        @NotNull
        private UserRole role;

        public List<Long> getUserIds() {
            return userIds;
        }

        public void setUserIds(List<Long> userIds) {
            this.userIds = userIds;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }
    }

    public static class BatchStatusDTO {
        @NotNull
        private List<Long> userIds;
        @NotNull
        private UserStatus status;

        public List<Long> getUserIds() {
            return userIds;
        }

        public void setUserIds(List<Long> userIds) {
            this.userIds = userIds;
        }

        public UserStatus getStatus() {
            return status;
        }

        public void setStatus(UserStatus status) {
            this.status = status;
        }
    }

    public static class UserDetailVO {
        private Long id;
        private String username;
        private String email;
        private UserRole role;
        private UserStatus status;
        private List<UserCourseVO> courses;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        private java.time.LocalDateTime lastLearningAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }

        public UserStatus getStatus() {
            return status;
        }

        public void setStatus(UserStatus status) {
            this.status = status;
        }

        public List<UserCourseVO> getCourses() {
            return courses;
        }

        public void setCourses(List<UserCourseVO> courses) {
            this.courses = courses;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public java.time.LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public java.time.LocalDateTime getLastLearningAt() {
            return lastLearningAt;
        }

        public void setLastLearningAt(java.time.LocalDateTime lastLearningAt) {
            this.lastLearningAt = lastLearningAt;
        }
    }

    public static class UserCourseVO {
        private Long courseId;
        private String title;
        private Integer progressPercent;

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(Integer progressPercent) {
            this.progressPercent = progressPercent;
        }
    }

    public static class AuditLogVO {
        private Long id;
        private String action;
        private String targetType;
        private Long targetId;
        private String detail;
        private java.time.LocalDateTime createdAt;
        private String adminUsername;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getTargetType() {
            return targetType;
        }

        public void setTargetType(String targetType) {
            this.targetType = targetType;
        }

        public Long getTargetId() {
            return targetId;
        }

        public void setTargetId(Long targetId) {
            this.targetId = targetId;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }
    }

    private Specification<UserEntity> buildUserSpec(String keyword, UserRole role, UserStatus status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Specification<AuditLogEntity> buildAuditSpec(String keyword, String action, String targetType) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("detail")), like),
                        cb.like(cb.lower(root.get("action")), like)
                ));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (targetType != null && !targetType.isBlank()) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void logAction(UserDetails userDetails, String action, String targetType, Long targetId, String detail) {
        if (userDetails == null) {
            return;
        }
        UserEntity admin = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (admin == null) {
            return;
        }
        AuditLogEntity log = new AuditLogEntity();
        log.setAdminUser(admin);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    private String safeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
