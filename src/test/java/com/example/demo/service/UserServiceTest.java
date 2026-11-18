package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenService tokenService;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder, jwtUtil, tokenService);
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest("admin", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名已存在");

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldEncodePassword() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(registerRequest("newuser", "123456"));

        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getRealName()).isEqualTo("测试用户");
        assertThat(saved.getPassword()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", saved.getPassword())).isTrue();
    }

    @Test
    void loginShouldRejectWrongPassword() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser("admin", "correct-pwd")));

        assertThatThrownBy(() -> userService.login(loginRequest("admin", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void loginShouldReturnToken() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser("admin", "correct-pwd")));
        when(jwtUtil.generateToken(1L, "admin", "USER")).thenReturn("jwt-token");

        LoginResponse resp = userService.login(loginRequest("admin", "correct-pwd"));

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getUserId()).isEqualTo(1L);
        assertThat(resp.getUsername()).isEqualTo("admin");
    }

    @Test
    void changePasswordShouldRejectWrongOldPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser("admin", "correct-pwd")));

        assertThatThrownBy(() -> userService.changePassword(1L, "wrong", "brand-new-pwd"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("原密码不正确");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordShouldEncodeNewPassword() {
        User user = existingUser("admin", "correct-pwd");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.changePassword(1L, "correct-pwd", "brand-new-pwd");

        assertThat(passwordEncoder.matches("brand-new-pwd", user.getPassword())).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileShouldRejectInvalidPhone() {
        User user = existingUser("admin", "correct-pwd");
        user.setPhone("13900139000");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfile(1L, null, null, "12345"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("手机号格式不正确");

        assertThat(user.getPhone()).isEqualTo("13900139000");
        verify(userRepository, never()).save(any());
    }

    private User existingUser(String username, String rawPassword) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return user;
    }

    private RegisterRequest registerRequest(String username, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setRealName("测试用户");
        return req;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }
}
