package ru.arkhipova.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.arkhipova.configuration.TestcontainersConfiguration;
import ru.arkhipova.model.request.UserLoginRequest;
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void registerAndLoginFlowWorks() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                .email("player@test.com")
                .username("player")
                .password("password123")
                .build();

        HttpRequest registerRequestHttp = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(registerRequest)))
                .build();
        HttpResponse<String> registerResponse =
                httpClient.send(registerRequestHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, registerResponse.statusCode());
        var registerBody = objectMapper.readTree(registerResponse.body());
        assertNotNull(registerBody.get("token").asText());
        assertEquals("player@test.com", registerBody.get("email").asText());

        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .email("player@test.com")
                .password("password123")
                .build();
        HttpRequest loginRequestHttp = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(loginRequest)))
                .build();
        HttpResponse<String> loginResponse = httpClient.send(loginRequestHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginResponse.statusCode());
        var loginBody = objectMapper.readTree(loginResponse.body());
        assertNotNull(loginBody.get("token").asText());
        assertEquals("player", loginBody.get("username").asText());
    }
}
