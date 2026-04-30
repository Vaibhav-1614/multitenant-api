package com.example.multitenantapi.auth;

import com.example.multitenantapi.entity.Tenant;
import com.example.multitenantapi.entity.User;
import com.example.multitenantapi.entity.UserRole;
import com.example.multitenantapi.repository.TenantRepository;
import com.example.multitenantapi.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        tenant = tenantRepository.save(Tenant.builder().name("Acme").plan("PRO").build());
    }

    @Test
    void registerSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new.user@acme.com");
        request.setPassword("pass1234");
        request.setRole(UserRole.ADMIN);
        request.setTenantId(tenant.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void registerDuplicateEmail() throws Exception {
        userRepository.save(User.builder()
                .email("dup@acme.com")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .role(UserRole.USER)
                .tenant(tenant)
                .build());

        RegisterRequest request = new RegisterRequest();
        request.setEmail("dup@acme.com");
        request.setPassword("pass1234");
        request.setRole(UserRole.ADMIN);
        request.setTenantId(tenant.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void loginSuccess() throws Exception {
        userRepository.save(User.builder()
                .email("login@acme.com")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .role(UserRole.USER)
                .tenant(tenant)
                .build());

        LoginRequest request = new LoginRequest();
        request.setEmail("login@acme.com");
        request.setPassword("pass1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresInMs").value(3600000));
    }

    @Test
    void loginWrongPassword() throws Exception {
        userRepository.save(User.builder()
                .email("wrong@acme.com")
                .passwordHash(passwordEncoder.encode("correctpass"))
                .role(UserRole.USER)
                .tenant(tenant)
                .build());

        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@acme.com");
        request.setPassword("badpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
