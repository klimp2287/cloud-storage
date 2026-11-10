package ru.netology.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.netology.dto.request.AuthenticationRQ;
import ru.netology.dto.response.AuthenticationRS;
import ru.netology.exception.UnauthorizedException;
import ru.netology.service.AuthenticationService;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public AuthenticationRS login(
            @Valid @RequestBody AuthenticationRQ authenticationRQ) {

        String token = authenticationService.login(
                authenticationRQ.getLogin(),
                authenticationRQ.getPassword()
        );

        return new AuthenticationRS(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("auth-token") String authToken) {

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid authentication token");
        }

        String token = authToken.substring(7);

        if (token.isBlank()) {
            throw new UnauthorizedException("Invalid authentication token");
        }

        authenticationService.logout(token);

        return ResponseEntity.ok(HttpStatus.OK);
    }
}