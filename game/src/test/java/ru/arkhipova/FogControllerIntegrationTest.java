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
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.arkhipova.model.request.FogUpdateRequest;
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.repository.EntitlementRepository;
import ru.arkhipova.repository.FloorRepository;
import ru.arkhipova.repository.FogChunkRepository;
import ru.arkhipova.repository.PlaythroughRepository;
import ru.arkhipova.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class FogControllerIntegrationTest {

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
    void fogEndpointsReturnAndUpdateFogData() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        String token = registerAndGetToken("fog@test.com", "fog_user");
        JsonNode playthrough = createPlaythrough(httpClient, token);
        String floorId = playthrough.get("currentFloorId").asText();

        HttpResponse<String> allFogResponse = authorizedGet(token, "/floors/" + floorId + "/fog");
        assertEquals(200, allFogResponse.statusCode());
        JsonNode allFog = objectMapper.readTree(allFogResponse.body());
        assertTrue(allFog.isArray());
        assertTrue(allFog.size() >= 1);

        HttpResponse<String> boundsResponse =
                authorizedGet(token, "/floors/" + floorId + "/fog/bounds?minX=0&minY=0&maxX=16&maxY=16");
        assertEquals(200, boundsResponse.statusCode());
        JsonNode boundsFog = objectMapper.readTree(boundsResponse.body());
        assertTrue(boundsFog.isArray());

        HttpResponse<String> revealResponse =
                authorizedPostNoBody(token, "/floors/" + floorId + "/fog/reveal?playerX=2&playerY=2&radius=3");
        assertEquals(200, revealResponse.statusCode());

        String updatePayload =
                """
                {"chunks":[{"chunkX":0,"chunkY":0,"maskBase64":"AQIDBA=="}]}
                """;
        sendFogUpdateByWebSocket(floorId, objectMapper.readValue(updatePayload, FogUpdateRequest.class));
        Thread.sleep(200);

        HttpResponse<String> afterUpdate = authorizedGet(token, "/floors/" + floorId + "/fog");
        assertEquals(200, afterUpdate.statusCode());
        JsonNode updated = objectMapper.readTree(afterUpdate.body());
        JsonNode firstChunk = updated.get(0);
        assertEquals("AQIDBA==", firstChunk.get("maskBase64").asText());
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

    private HttpResponse<String> authorizedPostNoBody(String token, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void sendFogUpdateByWebSocket(String floorId, FogUpdateRequest request)
            throws ExecutionException, InterruptedException {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {}

                    @Override
                    public void handleException(
                            StompSession session,
                            StompCommand command,
                            StompHeaders headers,
                            byte[] payload,
                            Throwable exception) {
                        throw new RuntimeException("STOMP error", exception);
                    }
                })
                .get();

        session.send("/app/floors/" + floorId + "/fog", request);
        session.disconnect();
    }
}
