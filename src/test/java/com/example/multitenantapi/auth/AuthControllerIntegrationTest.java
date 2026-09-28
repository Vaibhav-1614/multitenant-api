package com.example.multitenantapi.auth;

import com.example.multitenantapi.entity.Tenant;
import com.example.multitenantapi.entity.User;
import com.example.multitenantapi.entity.UserRole;
import com.example.multitenantapi.repository.ProjectRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        tenant = tenantRepository.save(Tenant.builder().name("Acme").plan("PRO").build());
    }

    @Test
    void registerCreatesTenantAndAdmin() throws Exception {
        RegisterRequest request = registerRequest("Globex", "founder@globex.com", "pass12345");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        User created = userRepository.findByEmail("founder@globex.com").orElseThrow();
        assertThat(created.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(created.getTenant().getId()).isNotEqualTo(tenant.getId());
        assertThat(tenantRepository.count()).isEqualTo(2);
    }

    @Test
    void registerCannotJoinExistingTenant() throws Exception {
        // Unknown fields such as tenantId/role are ignored: sign-up always creates a fresh tenant.
        String body = """
                {"tenantName":"Evil Corp","email":"attacker@evil.com","password":"pass12345",
                 "tenantId":%d,"role":"ADMIN"}
                """.formatted(tenant.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        User attacker = userRepository.findByEmail("attacker@evil.com").orElseThrow();
        assertThat(attacker.getTenant().getId()).isNotEqualTo(tenant.getId());
    }

    @Test
    void registerDuplicateEmail() throws Exception {
        userRepository.save(User.builder()
                .email("dup@acme.com")
                .passwordHash(passwordEncoder.encode("pass12345"))
                .role(UserRole.USER)
                .tenant(tenant)
                .build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("Dup Inc", "dup@acme.com", "pass12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("Short", "short@pw.com", "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: must be between 8 and 72 characters"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void loginSuccess() throws Exception {
        userRepository.save(User.builder()
                .email("login@acme.com")
                .passwordHash(passwordEncoder.encode("pass12345"))
                .role(UserRole.USER)
                .tenant(tenant)
                .build());

        LoginRequest request = new LoginRequest();
        request.setEmail("login@acme.com");
        request.setPassword("pass12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresInMs").value(3600000));
    }

    @Test
    void loginWrongPasswordReturns401() throws Exception {
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    private static RegisterRequest registerRequest(String tenantName, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setTenantName(tenantName);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
