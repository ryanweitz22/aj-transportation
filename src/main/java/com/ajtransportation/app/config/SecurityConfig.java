package com.ajtransportation.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF only for Ozow's server-to-server ITN endpoint
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/payment/notify")
            )

            .authorizeHttpRequests(auth -> auth
                // Public static and pages
                .requestMatchers(
                    "/", "/login", "/register", "/verify-email",
                    "/about", "/contact",
                    "/css/**", "/js/**", "/images/**", "/favicon.ico",
                    "/bookings", "/bookings/status/**", "/bookings/pending-count",
                    "/calculate-fare"
                ).permitAll()

                // Ozow ITN — must be public (server-to-server, no session)
                .requestMatchers("/payment/notify").permitAll()

                // Payment flow — authenticated users only
                .requestMatchers("/payment/**").authenticated()

                // Admin only
                .requestMatchers("/admin/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("identifier")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}