package com.yatayat.backend.security;

import com.yatayat.backend.config.GoogleOAuthHandler;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleOAuthHandlerTests {
    private UserRepository users; private GoogleOAuthHandler handler;

    @BeforeEach void setUp() {
        users = mock(UserRepository.class);
        handler = new GoogleOAuthHandler(users, "http://localhost:5173/google-success",
                "http://localhost:5173/login?googleError=oauth");
    }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void newGoogleIdentityCreatesOnePassengerAndAuthenticatedSession() throws Exception {
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(call -> { User saved = call.getArgument(0); saved.setId(1L); return saved; });
        MockHttpServletRequest request = new MockHttpServletRequest(); MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, oauth("New Passenger", "NEW@Example.com", true));

        verify(users, times(1)).save(any(User.class));
        User created = mockingDetails(users).getInvocations().stream().filter(i -> i.getMethod().getName().equals("save"))
                .map(i -> (User) i.getArgument(0)).findFirst().orElseThrow();
        assertThat(created.getRole()).isEqualTo("PASSENGER"); assertThat(created.getPassword()).isNull();
        assertThat(created.getWallet()).isNotNull(); assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/google-success");
        assertThat(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority()).containsExactly("ROLE_PASSENGER");
    }

    @Test void existingLocalAccountIsReusedWithoutChangingRoleOrPassword() throws Exception {
        User existing = new User("Operator", "operator@example.com", "9800000000", "encoded-local-password", "OPERATOR");
        when(users.findByEmailIgnoreCase("operator@example.com")).thenReturn(Optional.of(existing));
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(),
                oauth("Different Name", "operator@example.com", true));
        verify(users, never()).save(any()); assertThat(existing.getRole()).isEqualTo("OPERATOR");
        assertThat(existing.getPassword()).isEqualTo("encoded-local-password"); assertThat(existing.getFullName()).isEqualTo("Operator");
    }

    @Test void repeatedGoogleLoginDoesNotCreateDuplicate() throws Exception {
        User existing = new User("Passenger", "repeat@example.com", "", null, "PASSENGER");
        when(users.findByEmailIgnoreCase("repeat@example.com")).thenReturn(Optional.of(existing));
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), oauth("Passenger", "repeat@example.com", true));
        verify(users, never()).save(any());
    }

    @Test void missingEmailFailsSafelyWithoutProvisioning() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new UsernamePasswordAuthenticationToken(new DefaultOAuth2User(
                        java.util.List.of(new SimpleGrantedAuthority("OIDC_USER")), Map.of("name", "No Email"), "name"), null));
        verifyNoInteractions(users); assertThat(response.getRedirectedUrl()).contains("googleError=oauth");
    }

    private UsernamePasswordAuthenticationToken oauth(String name, String email, boolean verified) {
        DefaultOAuth2User principal = new DefaultOAuth2User(java.util.List.of(new SimpleGrantedAuthority("OIDC_USER")),
                Map.of("name", name, "email", email, "email_verified", verified), "email");
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
