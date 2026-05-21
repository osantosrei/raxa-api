package com.raxa.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raxa.dto.request.LoginRequest;
import com.raxa.dto.request.RegisterRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerLoginAndReadProfileWithJwt() throws Exception {
        String email = "integration-%s@example.com".formatted(UUID.randomUUID());
        RegisterRequest registerRequest = new RegisterRequest(
                "Usuario Integracao",
                email,
                "Password123!",
                "11999999999"
        );

        String registerJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode registerResponse = objectMapper.readTree(registerJson);

        assertThat(registerResponse.path("token").asText()).isNotBlank();
        assertThat(registerResponse.path("user").path("email").asText()).isEqualTo(email);

        LoginRequest loginRequest = new LoginRequest(email, "Password123!");
        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode loginResponse = objectMapper.readTree(loginJson);

        String token = loginResponse.path("token").asText();
        assertThat(token).isNotBlank();

        String profileJson = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode profileResponse = objectMapper.readTree(profileJson);

        assertThat(profileResponse.path("email").asText()).isEqualTo(email);
    }

    @Test
    void openApiExposesJwtBearerScheme() throws Exception {
        String openApiJson = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode openApi = objectMapper.readTree(openApiJson);

        assertThat(openApi.path("info").path("title").asText()).isEqualTo("Raxa API");
        assertThat(openApi.path("components")
                .path("securitySchemes")
                .path("bearerAuth")
                .path("scheme")
                .asText()).isEqualTo("bearer");
        assertThat(openApi.path("components")
                .path("securitySchemes")
                .path("bearerAuth")
                .path("bearerFormat")
                .asText()).isEqualTo("JWT");
        assertThat(openApi.path("paths").has("/api/v1/invites/{code}/resolve")).isTrue();
    }
}
