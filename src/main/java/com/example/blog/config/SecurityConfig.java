package com.example.blog.config;

import com.example.blog.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.POST, "/posts/*/like").permitAll()
                        .requestMatchers(HttpMethod.POST, "/posts", "/posts/update/**", "/posts/delete/**", "/posts/preview", "/posts/*/comments", "/posts/import", "/space/edit").authenticated()
                        .requestMatchers("/space", "/space/edit", "/space/drafts", "/space/export", "/space/export/download", "/posts/new", "/posts/edit/**").authenticated()
                        .requestMatchers("/", "/about", "/search", "/leaderboards", "/css/**", "/js/**", "/images/**", "/uploads/**", "/register", "/login", "/users/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts", "/posts/*", "/posts/search").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
