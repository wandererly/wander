package com.example.wander.user;

import com.example.wander.common.ApiResponse;
import com.example.wander.common.BizException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/role-requests")
public class RoleRequestController {

    private final UserRepository userRepository;
    private final RoleChangeRequestRepository roleChangeRequestRepository;

    public RoleRequestController(UserRepository userRepository,
                                 RoleChangeRequestRepository roleChangeRequestRepository) {
        this.userRepository = userRepository;
        this.roleChangeRequestRepository = roleChangeRequestRepository;
    }

    @PostMapping
    public ApiResponse<?> submit(@RequestBody @Valid RoleRequestDTO dto,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));
        if (dto.getTargetRole() == UserRole.ADMIN) {
            return ApiResponse.fail("不允许申请管理员");
        }
        if (user.getRole() == dto.getTargetRole()) {
            return ApiResponse.fail("已是该角色");
        }
        roleChangeRequestRepository.findByUserIdAndStatus(user.getId(), RoleChangeStatus.PENDING)
                .ifPresent(r -> {
                    throw new BizException("已有待处理申请");
                });
        RoleChangeRequestEntity req = new RoleChangeRequestEntity();
        req.setUser(user);
        req.setTargetRole(dto.getTargetRole());
        req.setReason(dto.getReason());
        req.setStatus(RoleChangeStatus.PENDING);
        roleChangeRequestRepository.save(req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/my")
    public ApiResponse<List<RoleRequestInfoVO>> myRequests(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));
        List<RoleRequestInfoVO> list = roleChangeRequestRepository.findByUserId(user.getId())
                .stream()
                .map(r -> {
                    RoleRequestInfoVO vo = new RoleRequestInfoVO();
                    vo.setId(r.getId());
                    vo.setTargetRole(r.getTargetRole());
                    vo.setStatus(r.getStatus());
                    vo.setReason(r.getReason());
                    vo.setAdminNote(r.getAdminNote());
                    vo.setNotified(r.isNotified());
                    return vo;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    public static class RoleRequestDTO {
        @NotNull
        private UserRole targetRole;
        private String reason;

        public UserRole getTargetRole() {
            return targetRole;
        }

        public void setTargetRole(UserRole targetRole) {
            this.targetRole = targetRole;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class RoleRequestInfoVO {
        private Long id;
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
}
