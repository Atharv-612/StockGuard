package com.stockguard.config;

import com.stockguard.entity.User;
import com.stockguard.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    @Value("${stockguard.security.cors.allowed-origins:http://localhost:8080,http://127.0.0.1:8080,http://localhost:3000,http://127.0.0.1:3000}")
    private String corsAllowedOrigins;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
            boolean isEnabled = user.getActive() != null ? user.getActive() : true;
            String rawRole = user.getRole() != null && !user.getRole().isBlank() ? user.getRole().toUpperCase() : "OWNER";

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if ("OWNER".equals(rawRole) || "SHOP_OWNER".equals(rawRole)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SHOP_OWNER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + rawRole));
            }

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPasswordHash(),
                    isEnabled,
                    true,
                    true,
                    true,
                    authorities
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                                "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://fonts.googleapis.com; " +
                                "font-src 'self' https://cdnjs.cloudflare.com https://fonts.gstatic.com; " +
                                "img-src 'self' data: https:; " +
                                "connect-src 'self'; " +
                                "frame-ancestors 'none';"
                        ))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required or token expired. Please log in again.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied. Insufficient permissions.\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Web UI and static assets
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/app.html",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/favicon.ico",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/logout",
                                "/api/settings/db-status"
                        ).permitAll()

                        // Shop Owner specific endpoints
                        .requestMatchers("/api/auth/users/**").hasAnyRole("SHOP_OWNER", "OWNER")
                        .requestMatchers("/api/audit/**").hasAnyRole("SHOP_OWNER", "OWNER")
                        .requestMatchers("/api/notifications/preferences").hasAnyRole("SHOP_OWNER", "OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/settings/shop").hasAnyRole("SHOP_OWNER", "OWNER")

                        // Management endpoints (SHOP_OWNER or MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER")
                        .requestMatchers("/api/inventory/adjust").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/batches/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER")
                        .requestMatchers("/api/reports/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER")
                        .requestMatchers("/api/notifications/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER")

                        // Point of Sale and general inventory reads (STAFF, MANAGER, SHOP_OWNER)
                        .requestMatchers("/api/sales/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/batches/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/alerts/**").hasAnyRole("SHOP_OWNER", "OWNER", "MANAGER", "STAFF")

                        // All other APIs require authentication
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new java.util.ArrayList<>();
        if (corsAllowedOrigins != null) {
            for (String origin : corsAllowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }
            }
        }

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
