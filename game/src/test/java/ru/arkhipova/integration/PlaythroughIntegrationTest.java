package ru.arkhipova;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
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
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.repository.EntitlementRepository;
import ru.arkhipova.repository.FloorRepository;
import ru.arkhipova.repository.FogChunkRepository;
import ru.arkhipova.repository.PlaythroughRepository;
import ru.arkhipova.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PlaythroughIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DiscoveredIconRepository discoveredIconRepository;

    @Autowired
    private FogChunkRepository fogChunkRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private PlaythroughRepository playthroughRepository;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void cleanup() {
        discoveredIconRepository.deleteAll();
        fogChunkRepository.deleteAll();
        floorRepository.deleteAll();
        playthroughRepository.deleteAll();
        entitlementRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createPlaythroughRequiresAuthentication() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/playthrough"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    void authenticatedPlayerCanCreateAndFetchPlaythrough() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        String token = registerAndGetToken("hero@test.com", "hero");
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/playthrough"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, createResponse.statusCode());
        JsonNode createBody = objectMapper.readTree(createResponse.body());
        assertNotNull(createBody.get("id").asText());
        assertEquals(1, createBody.get("currentFloorNumber").asInt());

        HttpRequest activeRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/playthrough/active"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> activeResponse = httpClient.send(activeRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, activeResponse.statusCode());
        JsonNode activeBody = objectMapper.readTree(activeResponse.body());
        assertEquals("ACTIVE", activeBody.get("status").asText());
    }

    private String registerAndGetToken(String email, String username) throws Exception {
        UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                .email(email)
                .username(username)
                .password("password123")
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(registerRequest)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("token").asText();
    }
}
