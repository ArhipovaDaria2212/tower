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
class FloorControllerIntegrationTest {

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
    void getCurrentFloorAndAdvanceFlowWorks() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        String token = registerAndGetToken("floor@test.com", "floor_user");
        createPlaythrough(httpClient, token);

        HttpResponse<String> currentFloorResponse = authorizedGet(httpClient, token, "/playthrough/active/floor");
        assertEquals(200, currentFloorResponse.statusCode());
        JsonNode currentFloor = objectMapper.readTree(currentFloorResponse.body());
        assertEquals(1, currentFloor.get("floorNumber").asInt());

        HttpResponse<String> advanceResponse =
                authorizedPostNoBody(httpClient, token, "/playthrough/active/floor/advance");
        assertEquals(200, advanceResponse.statusCode());
        JsonNode advanceBody = objectMapper.readTree(advanceResponse.body());
        assertEquals("Floor 2 requires purchase", advanceBody.get("message").asText());
        assertEquals("null", advanceBody.get("floor").asText());
    }

    private String registerAndGetToken(String email, String username) throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email(email)
                .username(username)
                .password("password123")
                .build();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode body = objectMapper.readTree(response.body());
        return body.get("token").asText();
    }

    private void createPlaythrough(HttpClient httpClient, String token) throws Exception {
        HttpResponse<String> response = authorizedPostNoBody(httpClient, token, "/playthrough");
        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get("id").asText());
    }

    private HttpResponse<String> authorizedGet(HttpClient client, String token, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> authorizedPostNoBody(HttpClient client, String token, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
