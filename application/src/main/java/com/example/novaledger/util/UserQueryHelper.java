package com.example.novaledger.util;

import com.example.novaledger.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 跨模組的 User 查詢輔助工具。
 *
 * <p>application 層 orchestration 用途，不屬於任何單一業務 module。
 * 未來若有三個以上使用點，考慮在 auth module 建立 UserQueryService 正式暴露。
 */
@Component
@RequiredArgsConstructor
public class UserQueryHelper {

    private final UserRepository userRepository;

    /**
     * 依 keyword 模糊查詢符合的 userId list。
     *
     * @param keyword 搜尋關鍵字（username / email 模糊比對）
     * @return null 代表不篩選（keyword 為空）；empty list 代表無符合 user
     */
    public List<Long> resolveUserIds(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        List<Long> userIds = userRepository.searchUsers(keyword, Pageable.unpaged())
                .stream()
                .map(u -> u.getId())
                .toList();
        return userIds;
    }
}
