package ru.arkhipova;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class MapIconControllerIntegrationTest {

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
    void iconDiscoveryEndpointsWork() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String token = registerAndGetToken("icons@test.com", "icons_user");
        String floorId = createPlaythrough(client, token).get("currentFloorId").asText();

        HttpResponse<String> discoverOneResponse =
                authorizedPostJson(token, "/floors/" + floorId + "/discovered-icons", "{\"iconId\":\"shrine\"}");
        assertEquals(200, discoverOneResponse.statusCode());
        assertEquals(
                "shrine",
                objectMapper.readTree(discoverOneResponse.body()).get("iconId").asText());

        String batchPayload = """
                [{"iconId":"altar"},{"iconId":"portal"}]
                """;
        HttpResponse<String> discoverBatchResponse =
                authorizedPostJson(token, "/floors/" + floorId + "/discovered-icons/batch", batchPayload);
        assertEquals(200, discoverBatchResponse.statusCode());

        HttpResponse<String> allIconsResponse = authorizedGet(token, "/floors/" + floorId + "/discovered-icons");
        assertEquals(200, allIconsResponse.statusCode());
        JsonNode iconIds = objectMapper.readTree(allIconsResponse.body()).get("discoveredIconIds");
        assertTrue(iconIds.isArray());
        assertTrue(iconIds.size() >= 3);
    }

    private String registerAndGetToken(String email, String username) throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email(email)
                .username(username)
                .password("password123")
                .build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();
        HttpResponse<String> response =
                HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode body = objectMapper.readTree(response.body());
        return body.get("token").asText();
    }

    private JsonNode createPlaythrough(HttpClient client, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/playthrough"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get("id").asText());
        return body;
    }

    private HttpResponse<String> authorizedGet(String token, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> authorizedPostJson(String token, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
