package com.yatayat.backend.config;

import com.yatayat.backend.entity.User;
import com.yatayat.backend.entity.Wallet;
import com.yatayat.backend.entity.AuthenticationProvider;
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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.net.URI;

public class GoogleOAuthHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthHandler.class);
    private static final Pattern SENSITIVE_DESCRIPTION = Pattern.compile(
            "(?i)(authorization[_ -]?code|access[_ -]?token|id[_ -]?token|client[_ -]?secret|cookie|@)");
    private final UserRepository userRepository;
    private final String successUrl;
    private final String failureUrl;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

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
        HttpSession existingSession = request.getSession(false);
        String mode = existingSession == null
                ? null : clean(existingSession.getAttribute("googleMode"));
        if (email == null || Boolean.FALSE.equals(verified)) {
            LOGGER.warn("Google OAuth callback rejected because the provider identity was incomplete.");
            clearMode(request); response.sendRedirect(failureUrl); return;
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if ("LOGIN".equals(mode)) {
            if (user == null) {
                finishWithoutLogin(request, response,
                        loginRedirect("googleError=notRegistered"));
                return;
            }
            if (user.getAuthenticationProvider() != AuthenticationProvider.GOOGLE) {
                finishWithoutLogin(request, response,
                        loginRedirect("googleError=localAccount"));
                return;
            }
            establishApplicationSession(request, response, user);
            return;
        }
        if ("REGISTER".equals(mode)) {
            if (user != null) {
                String query = user.getAuthenticationProvider() == AuthenticationProvider.LOCAL
                        ? "googleError=localAccount"
                        : "googleRegistered=alreadyExists";
                finishWithoutLogin(request, response, loginRedirect(query));
                return;
            }
            createPassenger(name, email);
            finishWithoutLogin(request, response, loginRedirect("googleRegistered=true"));
            return;
        }
        LOGGER.warn("Google OAuth callback rejected because its server-side intent was missing.");
        finishWithoutLogin(request, response, failureUrl);
    }

    private void establishApplicationSession(HttpServletRequest request,
                                             HttpServletResponse response,
                                             User user) throws IOException {
        if (user.getRole() == null || user.getRole().isBlank()) {
            LOGGER.error("Google OAuth callback found an account without a valid application role.");
            finishWithoutLogin(request, response, failureUrl);
            return;
        }
        UsernamePasswordAuthenticationToken applicationAuthentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase(Locale.ROOT))));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(applicationAuthentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(false);
        if (session == null) session = request.getSession(true); else request.changeSessionId();
        session.removeAttribute("googleMode");
        /*
         * The OAuth filter authenticated the Google principal, but this handler replaces it
         * with the application's email/role authentication. Persist that replacement through
         * Spring Security's repository so it survives the frontend redirect.
         */
        securityContextRepository.saveContext(context, request, response);
        LOGGER.info("Google OAuth session established for application role {}.", user.getRole());
        response.sendRedirect(successUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        String errorCode = exception instanceof OAuth2AuthenticationException oauthFailure
                ? oauthFailure.getError().getErrorCode() : "unknown";
        String description = exception instanceof OAuth2AuthenticationException oauthFailure
                ? safeDescription(oauthFailure.getError().getDescription()) : null;
        LOGGER.warn(
                "Google OAuth failed: errorCode={}, cause={}, path={}, callbackErrorPresent={}, description={}",
                errorCode, rootCauseName(exception), request.getRequestURI(),
                request.getParameter("error") != null,
                description == null ? "not-logged" : description);
        SecurityContextHolder.clearContext(); clearMode(request); response.sendRedirect(failureUrl);
    }

    private User createPassenger(String name, String email) {
        User user = new User(name == null ? "Google User" : name, email, "", null, "PASSENGER");
        user.setAuthenticationProvider(AuthenticationProvider.GOOGLE);
        user.setWallet(new Wallet(user));
        return userRepository.save(user);
    }
    private void finishWithoutLogin(HttpServletRequest request, HttpServletResponse response,
                                    String redirectUrl) throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("googleMode");
            session.removeAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        }
        response.sendRedirect(redirectUrl);
    }
    private String loginRedirect(String query) {
        URI frontend = URI.create(successUrl);
        return frontend.resolve("/login?" + query).toString();
    }
    private void clearMode(HttpServletRequest request) { HttpSession session = request.getSession(false); if (session != null) session.removeAttribute("googleMode"); }
    private String normalized(Object value) { String text = clean(value); return text == null ? null : text.toLowerCase(Locale.ROOT); }
    private String clean(Object value) { if (!(value instanceof String text) || text.isBlank()) return null; return text.trim(); }
    private String safeDescription(String description) {
        if (description == null || description.isBlank()
                || description.length() > 300
                || SENSITIVE_DESCRIPTION.matcher(description).find()) return null;
        return description.replaceAll("[\\r\\n\\t]", " ").trim();
    }
    private String rootCauseName(Throwable failure) {
        if (failure == null) return "unknown";
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        return cause.getClass().getSimpleName();
    }
}
