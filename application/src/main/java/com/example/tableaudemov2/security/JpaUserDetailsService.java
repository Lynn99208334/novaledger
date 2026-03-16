package com.example.tableaudemov2.security;

import com.example.tableaudemov2.common.exception.BusinessException;
import com.example.tableaudemov2.common.exception.ErrorCode;
import com.example.tableaudemov2.entity.User;
import com.example.tableaudemov2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Value("${app.auth.skip-email-verify:false}")
    private boolean skipEmailVerify;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                ErrorCode.USER_NOT_FOUND.getMessage()
                        )
                );
        System.out.println(">>> FOUND USER: " + user.getEmail() + ", verified=" + user.getEmailVerified() + ", password=" + user.getPassword());
        System.out.println(">>> PASSWORD MATCH: " + new BCryptPasswordEncoder().matches("password123", user.getPassword()));
        System.out.println(new BCryptPasswordEncoder().encode("password123"));

        // ✅ 關鍵：登入時擋未完成 Email 驗證的帳號
        if (!skipEmailVerify && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(
                    ErrorCode.EMAIL_NOT_VERIFIED
            );
        }

        // （選擇性）如果你未來有停權狀態
        // if (user.getStatus() != UserStatus.ACTIVE) {
        //     throw new RuntimeException("ACCOUNT_NOT_ACTIVE");
        // }

        return new SecurityUser(user);
    }
}
