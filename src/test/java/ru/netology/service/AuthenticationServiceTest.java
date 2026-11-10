package ru.netology.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import ru.netology.jwt.JwtTokenUtil;
import ru.netology.repository.AuthenticationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserService userService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void loginAuthenticatesUserAndStoresToken() {
        when(userService.loadUserByUsername("user@test.ru"))
                .thenReturn(userDetails);
        when(jwtTokenUtil.generateToken(userDetails))
                .thenReturn("jwt-token");

        String token = authenticationService.login(
                "user@test.ru",
                "password"
        );

        assertEquals("jwt-token", token);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(
                        "user@test.ru",
                        "password"
                )
        );

        verify(authenticationRepository)
                .putTokenAndUsername("jwt-token", "user@test.ru");
    }

    @Test
    void loginPropagatesAuthenticationFailure() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login("user@test.ru", "wrong")
        );

        verifyNoInteractions(jwtTokenUtil, authenticationRepository);
    }

    @Test
    void logoutRemovesTokenWithoutBearerPrefix() {
        authenticationService.logout("jwt-token");

        verify(authenticationRepository).getUsernameByToken("jwt-token");
        verify(authenticationRepository)
                .removeTokenAndUsernameByToken("jwt-token");
    }
}
