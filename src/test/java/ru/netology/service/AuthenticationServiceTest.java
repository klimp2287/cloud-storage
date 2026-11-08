package ru.netology.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import ru.netology.dto.request.AuthenticationRQ;
import ru.netology.dto.response.AuthenticationRS;
import ru.netology.jwt.JwtTokenUtil;
import ru.netology.repository.AuthenticationRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private UserService userService;
    @Mock private UserDetails userDetails;

    @InjectMocks private AuthenticationService authenticationService;

    @Test
    void loginAuthenticatesUserAndStoresToken() {
        AuthenticationRQ request = new AuthenticationRQ("user@test.ru", "password");
        when(userService.loadUserByUsername("user@test.ru")).thenReturn(userDetails);
        when(jwtTokenUtil.generateToken(userDetails)).thenReturn("jwt-token");

        AuthenticationRS response = authenticationService.login(request);

        assertEquals("jwt-token", response.getAuthToken());
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("user@test.ru", "password"));
        verify(authenticationRepository).putTokenAndUsername("jwt-token", "user@test.ru");
    }

    @Test
    void loginPropagatesAuthenticationFailure() {
        AuthenticationRQ request = new AuthenticationRQ("user@test.ru", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authenticationService.login(request));
        verifyNoInteractions(jwtTokenUtil, authenticationRepository);
    }

    @Test
    void logoutRemovesTokenWithoutBearerPrefix() {
        authenticationService.logout("Bearer jwt-token");

        verify(authenticationRepository).getUsernameByToken("jwt-token");
        verify(authenticationRepository).removeTokenAndUsernameByToken("jwt-token");
    }
}
