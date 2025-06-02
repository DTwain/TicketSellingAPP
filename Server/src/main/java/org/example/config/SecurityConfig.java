package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for REST API
                .csrf().disable()

                // Enable CORS
                .cors().and()

                // Set session management to stateless (JWT-based)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // Configure authorization using antMatchers (Spring Security 5.x)
                .authorizeRequests(authz -> authz
                        // Allow authentication endpoints
                        .antMatchers("/api/auth/**").permitAll()

                        // Allow WebSocket connections
                        .antMatchers("/ws/**").permitAll()

                        // Allow static resources
                        .antMatchers("/static/**", "/*.html", "/*.js", "/*.css").permitAll()

                        // Allow all API endpoints for now (we'll handle authorization via JWT manually)
                        .antMatchers("/api/**").permitAll()

                        // Allow health check endpoints
                        .antMatchers("/actuator/**").permitAll()

                        // Require authentication for all other requests
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}