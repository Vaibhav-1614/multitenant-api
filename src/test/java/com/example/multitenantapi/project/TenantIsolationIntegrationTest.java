package com.example.multitenantapi.project;

import com.example.multitenantapi.repository.ProjectRepository;
import com.example.multitenantapi.repository.TenantRepository;
import com.example.multitenantapi.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void cleanDatabase() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    void projectsAreInvisibleAcrossTenants() throws Exception {
        String acme = signUp("Acme", "admin@acme.com");
        String globex = signUp("Globex", "admin@globex.com");

        long acmeProjectId = createProject(acme, "Acme roadmap");
        createProject(globex, "Globex launch");

        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, bearer(acme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Acme roadmap"));

        // Another tenant cannot read, update or delete the project, even with its exact id.
        mockMvc.perform(get("/api/projects/" + acmeProjectId).header(HttpHeaders.AUTHORIZATION, bearer(globex)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/projects/" + acmeProjectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(globex))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hijacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/projects/" + acmeProjectId).header(HttpHeaders.AUTHORIZATION, bearer(globex)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/" + acmeProjectId).header(HttpHeaders.AUTHORIZATION, bearer(acme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme roadmap"));
    }

    @Test
    void projectCrudLifecycle() throws Exception {
        String token = signUp("Initech", "admin@initech.com");
        long id = createProject(token, "TPS reports");

        mockMvc.perform(put("/api/projects/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TPS reports v2\",\"description\":\"New cover sheet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TPS reports v2"));

        mockMvc.perform(delete("/api/projects/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownSortPropertyReturns400() throws Exception {
        String token = signUp("Umbrella", "admin@umbrella.com");

        mockMvc.perform(get("/api/projects?sort=doesNotExist").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property: doesNotExist"));
    }

    @Test
    void missingOrInvalidTokenReturns401Json() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));

        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminsCanAddTenantMembers() throws Exception {
        String admin = signUp("Hooli", "admin@hooli.com");

        mockMvc.perform(post("/api/tenants/me/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "dev@hooli.com", "password", "pass12345", "role", "USER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));

        String member = login("dev@hooli.com");

        mockMvc.perform(get("/api/tenants/me").header(HttpHeaders.AUTHORIZATION, bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hooli"));

        mockMvc.perform(post("/api/tenants/me/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "sneaky@hooli.com", "password", "pass12345", "role", "ADMIN"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tenants/me/users").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private String signUp(String tenantName, String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "tenantName", tenantName, "email", email, "password", "pass12345"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String login(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "pass12345"));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private long createProject(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
