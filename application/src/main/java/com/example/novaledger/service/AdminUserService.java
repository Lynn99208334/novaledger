package com.example.novaledger.service;

import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.enums.UserStatus;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.dto.AdminUserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<AdminUserDto> getUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.searchUsers(keyword, pageable)
                .map(AdminUserDto::from);
    }

    @Transactional
    public AdminUserDto disableUser(Long userId) {
        User user = findUserById(userId);
        if (user.getSystemAdmin()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "無法停用系統管理員");
        }
        user.setStatus(UserStatus.SUSPENDED);
        user.setEnabled(false);
        return AdminUserDto.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto enableUser(Long userId) {
        User user = findUserById(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        return AdminUserDto.from(userRepository.save(user));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
