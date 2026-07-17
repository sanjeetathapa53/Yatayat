package com.yatayat.backend.config;

import com.yatayat.backend.entity.User;
import com.yatayat.backend.entity.Wallet;
import com.yatayat.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GoogleOAuthHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private final UserRepository userRepository;
    private final String successUrl;
    private final String failureUrl;

    public GoogleOAuthHandler(UserRepository userRepository,
                              String successUrl, String failureUrl) {
        this.userRepository = userRepository;
        this.successUrl = successUrl;
        this.failureUrl = failureUrl;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = authentication.getPrincipal() instanceof OAuth2User user ? user : null;
        String email = oauthUser == null ? null : normalized(oauthUser.getAttribute("email"));
        String name = oauthUser == null ? null : clean(oauthUser.getAttribute("name"));
        Boolean verified = oauthUser == null ? null : oauthUser.getAttribute("email_verified");
        if (email == null || Boolean.FALSE.equals(verified)) {
            clearMode(request); response.sendRedirect(failureUrl); return;
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> createPassenger(name, email));
        UsernamePasswordAuthenticationToken applicationAuthentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase(Locale.ROOT))));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(applicationAuthentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(false);
        if (session == null) session = request.getSession(true); else request.changeSessionId();
        session.removeAttribute("googleMode");
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        response.sendRedirect(successUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext(); clearMode(request); response.sendRedirect(failureUrl);
    }

    private User createPassenger(String name, String email) {
        User user = new User(name == null ? "Google User" : name, email, "", null, "PASSENGER");
        user.setWallet(new Wallet(user));
        return userRepository.save(user);
    }
    private void clearMode(HttpServletRequest request) { HttpSession session = request.getSession(false); if (session != null) session.removeAttribute("googleMode"); }
    private String normalized(Object value) { String text = clean(value); return text == null ? null : text.toLowerCase(Locale.ROOT); }
    private String clean(Object value) { if (!(value instanceof String text) || text.isBlank()) return null; return text.trim(); }
}
