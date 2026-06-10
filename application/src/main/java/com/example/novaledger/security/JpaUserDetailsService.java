package com.example.novaledger.security;

import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new UsernameNotFoundException(ErrorCode.USER_NOT_FOUND.getMessage())
                    );

            if (!skipEmailVerify && !Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
            }
            return new SecurityUser(user);
        } catch (Exception e) {
            throw e;
        }
    }
}
