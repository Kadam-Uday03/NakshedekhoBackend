package com.nakshedekho.config;

import com.nakshedekho.security.JwtAuthenticationFilter;
import com.nakshedekho.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
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
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // ── Security Response Headers ────────────────────────────────────────────
                .headers(headers -> headers
                        // Prevent clickjacking
                        .frameOptions(frame -> frame.sameOrigin())
                        // Prevent MIME-type sniffing
                        .contentTypeOptions(cto -> {})
                        // Content-Security-Policy — blocks inline scripts injected via XSS
                        // This protects the JWT stored in localStorage from script-based theft
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' https://checkout.razorpay.com https://accounts.google.com https://www.googletagmanager.com https://fonts.googleapis.com 'unsafe-inline'; " +
                                "style-src 'self' https://fonts.googleapis.com 'unsafe-inline'; " +
                                "font-src 'self' https://fonts.gstatic.com data:; " +
                                "img-src 'self' data: https: blob:; " +
                                "connect-src 'self' https://api.razorpay.com https://accounts.google.com; " +
                                "frame-src https://checkout.razorpay.com https://api.razorpay.com; " +
                                "object-src 'none'; " +
                                "base-uri 'self'"
                        ))
                )
                .authorizeHttpRequests(auth -> auth
                        // ── Static frontend files ── publicly accessible
                        // Note: Spring Security 6 PathPatternParser does NOT allow /** followed by more segments
                        .requestMatchers(
                                "/", "/index.html", "/login.html", "/register-customer.html",
                                "/about.html", "/blog.html", "/contact.html", "/packages.html",
                                "/payment.html", "/projects.html", "/services.html",
                                "/css/**", "/js/**", "/images/**", "/fonts/**",
                                "/favicon.ico", "/manifest.json",
                                "/customer/**", "/manager/**", "/owner/**"
                        ).permitAll()
                        // ── Public API ──────────────────────────────────────────────────────────
                        .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                        // ── Protected API ───────────────────────────────────────────────────────
                        .requestMatchers("/api/customer/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers("/api/blog/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_MANAGER_ADMIN", "ROLE_OWNER_ADMIN")
                        .requestMatchers("/api/files/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_MANAGER_ADMIN", "ROLE_OWNER_ADMIN")
                        .requestMatchers("/api/payment/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_MANAGER_ADMIN", "ROLE_OWNER_ADMIN")
                        .requestMatchers("/api/manager/**").hasAuthority("ROLE_MANAGER_ADMIN")
                        .requestMatchers("/api/owner/**").hasAuthority("ROLE_OWNER_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, authException) -> response
                                .sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use allowedOriginPatterns to allow any frontend domain (like your Hostinger domain) to communicate with API
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

