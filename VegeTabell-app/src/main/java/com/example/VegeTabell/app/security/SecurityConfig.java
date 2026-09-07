package com.example.VegeTabell.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RoleMatchAuthenticationSuccessHandler roleMatchAuthenticationSuccessHandler;

    public SecurityConfig(RoleMatchAuthenticationSuccessHandler roleMatchAuthenticationSuccessHandler) {
        this.roleMatchAuthenticationSuccessHandler = roleMatchAuthenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/signup", "/error",
                        "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // /reservations/{id}/cancel のみ買い手・売り手どちらもアクセスしうるため、
                // ロールでの制御ではなくコントローラー側の所有者チェックに委ねる（auth-design.md §2の補足に対応）。
                // より広い /reservations/** のBUYER限定ルールより先に評価させる必要がある。
                .requestMatchers(HttpMethod.POST, "/reservations/*/cancel").authenticated()
                .requestMatchers("/", "/products/**", "/reservations/**", "/mypage").hasRole("BUYER")
                .requestMatchers("/seller/**").hasRole("SELLER")
                .requestMatchers("/notifications/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(roleMatchAuthenticationSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );
            // .csrf(...) は意図的に触らない。デフォルト有効のままでauth-design.md §4の方針と一致

        return http.build();
    }
}
