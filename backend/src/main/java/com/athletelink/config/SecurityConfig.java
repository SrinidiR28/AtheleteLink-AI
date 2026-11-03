package com.athletelink.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt - never store plain-text passwords
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // stateless REST API using JWT, not cookies
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/index.html", "/admin", "/admin.html", "/js/**", "/css/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Trial catalog/details and public opportunities/announcements are public read-only data.
                        .requestMatchers(HttpMethod.GET, "/api/trials/**").permitAll()
<<<<<<< HEAD
                        .requestMatchers(HttpMethod.GET, "/api/athletes/*/profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/athletes/*/scouting-card").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/athletes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/opportunities").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/announcements").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/challenges/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recovery/injury-guidelines").permitAll()
                        .requestMatchers("/api/confirm-booking", "/api/trials/applications/confirm", "/api/trials/checkin").permitAll()
=======
                        .requestMatchers(HttpMethod.GET, "/api/opportunities").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/announcements").permitAll()
                        .requestMatchers("/api/confirm-booking", "/api/trials/applications/confirm").permitAll()
>>>>>>> 1bdcedfdb8d69998a11cf16735225a5f5770ab40
                        .requestMatchers("/api/health", "/api/stats", "/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
