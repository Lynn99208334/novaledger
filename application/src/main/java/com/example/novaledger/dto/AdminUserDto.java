package com.example.novaledger.dto;

import com.example.novaledger.auth.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserDto {

    private Long id;
    private String username;
    private String email;
    private Boolean emailVerified;
    private String status;
    private Boolean enabled;
    private Boolean isSystemAdmin;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static AdminUserDto from(User user) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setStatus(user.getStatus().name());
        dto.setEnabled(user.getEnabled());
        dto.setIsSystemAdmin(user.getSystemAdmin());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
