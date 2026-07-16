package com.yatayat.backend.config;

import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

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
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/bookings/validate-qr",
                                "/api/bookings/mark-used"
                        ).hasRole("DRIVER")
                        .requestMatchers("/api/bookings/**").hasRole("PASSENGER")
                        .requestMatchers("/api/wallet/**").hasRole("PASSENGER")
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
                        .successHandler((request, response, authentication) -> {

                            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

                            String email = oauthUser.getAttribute("email");
                            String name = oauthUser.getAttribute("name");

                            String mode = (String) request.getSession().getAttribute("googleMode");

                            if (mode == null) {
                                mode = "LOGIN";
                            }

                            Optional<User> existingUser = userRepository.findByEmail(email);
                            User user;

                            if ("LOGIN".equals(mode)) {

                                if (existingUser.isEmpty()) {
                                    request.getSession().removeAttribute("googleMode");
                                    response.sendRedirect("http://localhost:5173/login?googleError=notRegistered");
                                    return;
                                }

                                user = existingUser.get();

                                UsernamePasswordAuthenticationToken applicationAuthentication =
                                        new UsernamePasswordAuthenticationToken(
                                                user.getEmail(),
                                                null,
                                                List.of(new SimpleGrantedAuthority(
                                                        "ROLE_" + user.getRole().toUpperCase()
                                                ))
                                        );
                                SecurityContext securityContext =
                                        SecurityContextHolder.createEmptyContext();
                                securityContext.setAuthentication(applicationAuthentication);
                                SecurityContextHolder.setContext(securityContext);
                                request.getSession(true).setAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY,
                                        securityContext
                                );

                                String redirectUrl =
                                        "http://localhost:5173/google-success"
                                                + "?id=" + user.getId()
                                                + "&fullName=" + URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8)
                                                + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8)
                                                + "&phone=" + URLEncoder.encode(user.getPhone() == null ? "" : user.getPhone(), StandardCharsets.UTF_8)
                                                + "&role=" + user.getRole();

                                request.getSession().removeAttribute("googleMode");
                                response.setStatus(HttpServletResponse.SC_FOUND);
                                response.sendRedirect(redirectUrl);
                                return;
                            }

                            if ("REGISTER".equals(mode)) {

                                if (existingUser.isPresent()) {
                                    request.getSession().removeAttribute("googleMode");
                                    response.sendRedirect("http://localhost:5173/login?googleRegistered=alreadyExists");
                                    return;
                                }

                                user = new User(
                                        name,
                                        email,
                                        "",
                                        passwordEncoder.encode("GOOGLE_LOGIN"),
                                        "PASSENGER"
                                );

                                userRepository.save(user);

                                request.getSession().removeAttribute("googleMode");
                                response.sendRedirect("http://localhost:5173/login?googleRegistered=true");
                            }
                        })
                );

        return http.build();
    }
}
