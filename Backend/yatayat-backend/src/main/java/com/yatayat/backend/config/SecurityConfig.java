package com.yatayat.backend.config;

import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/api/auth/**",
                                "/login/**",
                                "/oauth2/**"
                        ).permitAll()
                        .anyRequest().permitAll()
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