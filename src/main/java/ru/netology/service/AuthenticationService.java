package ru.netology.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.netology.jwt.JwtTokenUtil;
import ru.netology.repository.AuthenticationRepository;

@Service
@AllArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationRepository authenticationRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    public String login(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        UserDetails userDetails = userService.loadUserByUsername(username);
        String token = jwtTokenUtil.generateToken(userDetails);

        authenticationRepository.putTokenAndUsername(token, username);

        log.info("User {} authentication. JWT: {}", username);

        return token;
    }

    public void logout(String token) {
        String username = authenticationRepository.getUsernameByToken(token);

        log.info("User {} logout. JWT is disabled.", username);

        authenticationRepository.removeTokenAndUsernameByToken(token);
    }
}