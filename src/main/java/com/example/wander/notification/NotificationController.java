package com.example.wander.notification;

import com.example.wander.common.ApiResponse;
import com.example.wander.common.BizException;
import com.example.wander.user.UserEntity;
import com.example.wander.user.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                  UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<NotificationVO>> list(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));
        Long userId = Objects.requireNonNull(user.getId(), "user id");
        List<NotificationVO> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> {
                    NotificationVO vo = new NotificationVO();
                    vo.setId(Objects.requireNonNull(n.getId(), "notification id"));
                    vo.setTitle(n.getTitle());
                    vo.setContent(n.getContent());
                    vo.setReadFlag(n.isReadFlag());
                    vo.setCreatedAt(n.getCreatedAt());
                    return vo;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));
        Long userId = Objects.requireNonNull(user.getId(), "user id");
        long count = notificationRepository.countByUserIdAndReadFlagFalse(userId);
        return ApiResponse.ok(count);
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<?> markRead(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        Long safeId = Objects.requireNonNull(id, "id");
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));
        NotificationEntity n = notificationRepository.findById(safeId)
                .orElseThrow(() -> new BizException("通知不存在"));
        Objects.requireNonNull(n.getId(), "notification id");
        Long userId = Objects.requireNonNull(user.getId(), "user id");
        UserEntity notifyUser = Objects.requireNonNull(n.getUser(), "notify user");
        if (!Objects.requireNonNull(notifyUser.getId(), "notify user id").equals(userId)) {
            throw new BizException("无权限");
        }
        n.setReadFlag(true);
        notificationRepository.save(n);
        return ApiResponse.ok(null);
    }

    public static class NotificationVO {
        private Long id;
        private String title;
        private String content;
        private boolean readFlag;
        private java.time.LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isReadFlag() {
            return readFlag;
        }

        public void setReadFlag(boolean readFlag) {
            this.readFlag = readFlag;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
