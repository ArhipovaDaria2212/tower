package ru.arkhipova.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import ru.arkhipova.configuration.TestcontainersConfiguration;
import ru.arkhipova.model.dto.FogChunkDto;
import ru.arkhipova.model.request.FogRevealRequest;
import ru.arkhipova.model.request.FogUpdateRequest;
import ru.arkhipova.model.request.UserRegisterRequest;
import ru.arkhipova.model.response.PlaythroughResponse;
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
        fogChunkRepository.deleteAll();
        floorRepository.deleteAll();
        playthroughRepository.deleteAll();
        entitlementRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Verifies the only HTTP fog endpoint (initial snapshot) plus the WS update path —
     * the bounds/reveal endpoints have been moved to STOMP and are covered separately.
     */
    @Test
    void httpSnapshotAndWebsocketUpdateRoundTrip() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        String token = registerAndGetToken("fog@test.com", "fog_user");
        PlaythroughResponse playthrough =
                objectMapper.convertValue(createPlaythrough(httpClient, token), PlaythroughResponse.class);
        UUID floorId = playthrough.getCurrentFloorId();

        HttpResponse<String> snapshot = authorizedGet(token, "/floors/" + floorId + "/fog");
        assertEquals(200, snapshot.statusCode());
        JsonNode body = objectMapper.readTree(snapshot.body());
        assertTrue(body.isArray());
        assertFalse(body.isEmpty());

        FogUpdateRequest update = objectMapper.readValue(
                "{\"chunks\":[{\"chunkX\":0,\"chunkY\":0,\"maskBase64\":\"AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=\"}]}",
                FogUpdateRequest.class);
        sendFogUpdateByWebSocket(token, floorId, update);
        Thread.sleep(200);

        HttpResponse<String> after = authorizedGet(token, "/floors/" + floorId + "/fog");
        assertEquals(200, after.statusCode());
        JsonNode updated = objectMapper.readTree(after.body());
        assertEquals(
                "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=",
                updated.get(0).get("maskBase64").asText());
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

    private void sendFogUpdateByWebSocket(String token, UUID floorId, FogUpdateRequest request) throws Exception {

        WebSocketStompClient stompClient = createSockJsClient();

        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + token);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSession session = stompClient
                .connectAsync(
                        "http://localhost:" + port + "/ws",
                        httpHeaders,
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);

        session.send("/app/floors/" + floorId + "/fog", request);

        session.disconnect();
    }

    private WebSocketStompClient createSockJsClient() {
        SockJsClient sockJsClient = new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())));

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        return stompClient;
    }
}
