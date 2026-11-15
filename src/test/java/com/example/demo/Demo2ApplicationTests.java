package com.example.demo;

import com.example.demo.security.JwtAuthFilter;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class Demo2ApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
        assertThat(userService).isNotNull();
        assertThat(jwtAuthFilter).isNotNull();
    }

    @Test
    void dataInitializerShouldBeDisabledInTestProfile() {
        assertThat(context.containsBean("dataInitializer")).isFalse();
    }
}
