package com.example.tableaudemov2.service;

import com.example.tableaudemov2.entity.User;
import com.example.tableaudemov2.enums.ErrorCode;
import com.example.tableaudemov2.exception.BusinessException;
import com.example.tableaudemov2.repository.UserRepository;
import com.example.tableaudemov2.security.JpaUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaUserDetailsService userDetailsService;

    @Test
    void should_throw_exception_when_email_not_verified() {

        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("123456");
        user.setEmailVerified(false);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userDetailsService.loadUserByUsername("test@example.com")
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }
}
