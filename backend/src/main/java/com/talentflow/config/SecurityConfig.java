package com.talentflow.config;

import java.util.Arrays;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/hr/**").hasRole("ADMIN")
                        .requestMatchers("/", "/index.html", "/register", "/profile", "/hr",
                                "/assets/**", "/favicon.svg", "/icons.svg").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${spring.security.user.name}") String username,
            @Value("${spring.security.user.password}") String password,
            @Value("${spring.security.user.roles:ADMIN}") String configuredRoles,
            PasswordEncoder passwordEncoder) {
        String[] roles = Arrays.stream(configuredRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toArray(String[]::new);
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(passwordEncoder.encode(password))
                        .roles(roles)
                        .build());
    }
}
