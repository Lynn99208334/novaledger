package com.example.tableaudemov2.config;

import com.example.tableaudemov2.adapter.cache.RedisCacheAdapter;
import com.example.tableaudemov2.security.JwtAuthenticationFilter;
import com.example.tableaudemov2.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            RedisCacheAdapter redisCacheAdapter,
            JwtUtil jwtUtil
    ) {
        return new JwtAuthenticationFilter(redisCacheAdapter, jwtUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("==== SecurityConfig loaded ===="); // ← 加這行

        http
                .csrf(csrf -> csrf.disable())

                // ✅ 表單登入要用 SESSION（先不要 JWT）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // ✅ 關掉 httpBasic
                // ❌ .httpBasic()

//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll()  // 暫時全開
//                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/info",
                                "/login",
                                "/register",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/debug/**",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                // Swagger UI
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs"        // ← 加這行，不帶 **
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // ✅ 啟用表單登入
                .formLogin(form -> form
                        .loginPage("/login")              // GET → login.html
                        .loginProcessingUrl("/login")    // POST → Spring Security
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
