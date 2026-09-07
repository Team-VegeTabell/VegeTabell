package com.example.VegeTabell.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
