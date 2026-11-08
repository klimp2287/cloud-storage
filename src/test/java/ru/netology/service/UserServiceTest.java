package ru.netology.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.netology.exception.UnauthorizedException;
import ru.netology.model.User;
import ru.netology.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Test
    void loadUserByUsernameReturnsExistingUser() {
        User user = User.builder().username("user@test.ru").password("encoded").build();
        when(userRepository.findByUsername("user@test.ru")).thenReturn(user);

        assertSame(user, userService.loadUserByUsername("user@test.ru"));
    }

    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing@test.ru")).thenReturn(null);

        assertThrows(UnauthorizedException.class,
                () -> userService.loadUserByUsername("missing@test.ru"));
    }
}
