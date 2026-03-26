package com.example.wander.user;

import com.example.wander.common.ApiResponse;
import com.example.wander.common.BizException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoVO> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new BizException("未登录");
        }
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return ApiResponse.ok(vo);
    }

    @PutMapping("/me")
    public ApiResponse<?> updateMe(@RequestBody @Valid UpdateProfileDTO dto,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new BizException("未登录");
        }
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            userRepository.findByEmail(dto.getEmail())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(user.getId())) {
                            throw new BizException("邮箱已存在");
                        }
                    });
            user.setEmail(dto.getEmail());
        }
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            String username = dto.getUsername().trim();
            userRepository.findByUsername(username)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(user.getId())) {
                            throw new BizException("用户名已存在");
                        }
                    });
            user.setUsername(username);
        }
        if (dto.getNickname() != null) {
            String nickname = dto.getNickname().isBlank() ? null : dto.getNickname();
            if (nickname != null) {
                userRepository.findByNickname(nickname)
                        .ifPresent(existing -> {
                            if (!existing.getId().equals(user.getId())) {
                                throw new BizException("该昵称已被使用，请换一个");
                            }
                        });
            }
            user.setNickname(nickname);
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone().isBlank() ? null : dto.getPhone());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl().isBlank() ? null : dto.getAvatarUrl());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userRepository.save(user);
        return ApiResponse.ok(null);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AvatarVO> uploadAvatar(@RequestParam("file") MultipartFile file,
                                              @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        if (userDetails == null) {
            throw new BizException("未登录");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择头像文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException("仅支持图片格式");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BizException("头像大小不能超过 2MB");
        }
        UserEntity user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BizException("用户不存在"));

        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;
        Path uploadDir = Path.of("./data/uploads");
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(filename);
        file.transferTo(target);

        String url = "/uploads/" + filename;
        user.setAvatarUrl(url);
        userRepository.save(user);

        AvatarVO vo = new AvatarVO();
        vo.setAvatarUrl(url);
        return ApiResponse.ok(vo);
    }

    public static class UserInfoVO {
        private Long id;
        private String username;
        private String email;
        private UserRole role;
        private UserStatus status;
        private String nickname;
        private String phone;
        private String avatarUrl;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;

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

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
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
    }

    public static class UpdateProfileDTO {
        @Email
        private String email;
        @Size(min = 4, max = 16)
        private String username;
        @Size(max = 50)
        private String nickname;
        @Size(max = 30)
        private String phone;
        @Size(max = 300)
        private String avatarUrl;
        @Size(min = 6, max = 50)
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class AvatarVO {
        private String avatarUrl;

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }
}
