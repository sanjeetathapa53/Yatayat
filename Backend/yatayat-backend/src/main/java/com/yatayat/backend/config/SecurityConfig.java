package com.yatayat.backend.config;

import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${yatayat.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList()
        );
        configuration.setAllowedMethods(
                List.of(
                        "GET", "POST", "PUT", "PATCH",
                        "DELETE", "OPTIONS"
                )
        );
        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Accept",
                "X-Requested-With"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(
                "/api/**",
                configuration
        );
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, GoogleOAuthHandler googleOAuthHandler) throws Exception {

        http
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/api/**"
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/error",
                                "/login/**",
                                "/oauth2/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/google-login",
                                "/api/auth/google-register"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/auth/login"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/auth/logout"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/send-otp",
                                "/api/auth/verify-otp",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/send-forgot-password-otp",
                                "/api/auth/reset-password"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/passenger/**").hasRole("PASSENGER")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/bookings/validate-qr",
                                "/api/bookings/mark-used"
                        ).hasRole("DRIVER")
                        .requestMatchers("/api/bookings/**").hasRole("PASSENGER")
                        .requestMatchers("/api/wallet/**").hasRole("PASSENGER")
                        .requestMatchers("/api/passenger/live-trips/**").hasRole("PASSENGER")
                        .requestMatchers("/api/passenger/live-trips").hasRole("PASSENGER")
                        .requestMatchers("/api/drivers/**").hasRole("DRIVER")
                        .requestMatchers("/api/driver/**").hasRole("DRIVER")
                        .requestMatchers("/api/operators/**").hasRole("OPERATOR")
                        .requestMatchers("/api/operator/**").hasRole("OPERATOR")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())
                        )
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(googleOAuthHandler)
                        .failureHandler(googleOAuthHandler));

        return http.build();
    }

    @Bean
    public GoogleOAuthHandler googleOAuthHandler(
            @Value("${yatayat.oauth.frontend-success-url:http://localhost:5173/google-success}") String successUrl,
            @Value("${yatayat.oauth.frontend-failure-url:http://localhost:5173/login?googleError=oauth}") String failureUrl) {
        return new GoogleOAuthHandler(userRepository, successUrl, failureUrl);
    }
}
