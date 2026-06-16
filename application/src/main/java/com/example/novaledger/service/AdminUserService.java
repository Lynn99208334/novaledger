package com.example.novaledger.service;

import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.enums.UserStatus;
import com.example.novaledger.auth.repository.TenantRepository;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.dto.AdminUserDto;
import com.example.novaledger.finance.importjob.repository.UploadJobRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UploadJobRepository uploadJobRepository;

    public AdminUserService(UserRepository userRepository,
                            TenantRepository tenantRepository,
                            UploadJobRepository uploadJobRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.uploadJobRepository = uploadJobRepository;
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

    /**
     * [DEV ONLY] 硬刪除使用者及其所有關聯資料。
     * 刪除順序：upload_jobs（RESTRICT FK）→ tenants（CASCADE 帶走所有租戶資料）→ users
     * 僅在 @Profile("dev") 的 Controller 呼叫，不對外暴露。
     */
    @Transactional
    public void hardDeleteUser(Long userId) {
        User user = findUserById(userId);
        if (user.getSystemAdmin()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "無法刪除系統管理員");
        }

        // 1. 先刪 upload_jobs（FK RESTRICT，不會 CASCADE）
        uploadJobRepository.deleteByCreatedBy(userId);

        // 2. 刪除該 user 擁有的 tenant（CASCADE 會帶走所有租戶下的資料）
        tenantRepository.deleteByOwnerUserId(userId);

        // 3. 最後刪 user（user_tenants 已 CASCADE 刪除）
        userRepository.deleteById(userId);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
