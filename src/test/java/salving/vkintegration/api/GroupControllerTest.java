package salving.vkintegration.api;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import salving.vkintegration.api.GroupController.GroupCheckRequest;
import salving.vkintegration.api.GroupController.GroupCheckResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GroupControllerTest {
    @Autowired
    private GroupController controller;
    @Autowired
    private WebTestClient testClient;

    @Autowired
    private CacheManager cacheManager;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setupMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        mockWebServer.close();
    }

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("users")
                .invalidate();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("vk.api.url", () -> mockWebServer.url("/")
                .url()
                .toString());

    }

    @Test
    public void validRequestReturnsExpectedResponse() {
        GroupCheckRequest requestBody = new GroupCheckRequest("testUser", "testGroup");
        GroupCheckResponse expectedResponse = new GroupCheckResponse("testLastName", "testFirstName", "testMiddleName",
                true);
        mockWebServer.setDispatcher(createValidVKDispatcher());

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .header("vk_service_token", "testToken")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GroupCheckResponse.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    public void validRequestCachesResponse() {
        GroupCheckRequest requestBody = new GroupCheckRequest("testUser", "testGroup");
        mockWebServer.setDispatcher(createValidVKDispatcher());

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .header("vk_service_token", "testToken")
                .exchange();

        Cache.ValueWrapper cachedResponse = cacheManager.getCache("users")
                .get(requestBody);
        assertNotNull(cachedResponse);
    }

    @Test
    public void requestWithoutTokenReturnsError() {
        GroupCheckRequest requestBody = new GroupCheckRequest("testUser", "testGroup");
        mockWebServer.setDispatcher(createValidVKDispatcher());

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    public void requestWithoutUserIdReturnsError() {
        GroupCheckRequest requestBody = new GroupCheckRequest("", "testGroup");
        mockWebServer.setDispatcher(createValidVKDispatcher());

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .header("vk_service_token", "testToken")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    public void requestWithoutGroupIdReturnsError() {
        GroupCheckRequest requestBody = new GroupCheckRequest("testUser", "");
        mockWebServer.setDispatcher(createValidVKDispatcher());

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .header("vk_service_token", "testToken")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    public void requestWithLongUserIdReturnsError() {
        GroupCheckRequest requestBody = new GroupCheckRequest("longUserId".repeat(4), "testGroup");
        mockWebServer.setDispatcher(createValidVKDispatcher());

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .header("vk_service_token", "testToken")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    public void requestWithLongGroupIdReturnsError() {
        GroupCheckRequest requestBody = new GroupCheckRequest("testUser", "longGroupId".repeat(4));
        mockWebServer.setDispatcher(createValidVKDispatcher());

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .header("vk_service_token", "testToken")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    public void requestWithInvalidUserIdReturnsError() {
        GroupCheckRequest requestBody = new GroupCheckRequest("1234_1234", "testGroup");
        Map<String, MockResponse> responseMap = new HashMap<>();
        responseMap.put("/users.get", new MockResponse().setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                            {
                              "response": [
                                {
                                  "id": "1234_1234",
                                  "nickname": "",
                                  "first_name": "testFirstName",
                                  "last_name": "testLastName",
                                  "can_access_closed": true,
                                  "is_closed": false
                                }
                              ]
                            }
                        """));
        responseMap.put("/groups.isMember", new MockResponse().setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                            {
                                "error": {
                                    "error_code": 100,
                                    "error_msg": "One of the parameters specified was missing or invalid: user_id not integer",
                                    "request_params": []
                                }
                            }
                        """));
        mockWebServer.setDispatcher(createVKDispatcher(responseMap));

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .header("vk_service_token", "testToken")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    public void noUserReturnsError() {
        GroupCheckRequest requestBody = new GroupCheckRequest("testUser", "testGroup");
        Map<String, MockResponse> responseMap = new HashMap<>();
        responseMap.put("/users.get", new MockResponse().setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                            {
                                "response": []
                            }
                        """));
        responseMap.put("/groups.isMember", new MockResponse().setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                            {
                                "response": 0
                            }
                        """));
        mockWebServer.setDispatcher(createVKDispatcher(responseMap));

        testClient.method(HttpMethod.GET)
                .uri("/group/check")
                .bodyValue(requestBody)
                .header("vk_service_token", "testToken")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @NotNull
    private static Dispatcher createValidVKDispatcher() {
        Map<String, MockResponse> responseMap = new HashMap<>();

        responseMap.put("/users.get", new MockResponse().setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                            {
                              "response": [
                                {
                                  "id": "testUser",
                                  "nickname": "testMiddleName",
                                  "first_name": "testFirstName",
                                  "last_name": "testLastName",
                                  "can_access_closed": true,
                                  "is_closed": false
                                }
                              ]
                            }
                        """));

        responseMap.put("/groups.isMember", new MockResponse().setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                            {
                                "response": 1
                            }
                        """));

        return createVKDispatcher(responseMap);
    }

    @NotNull
    private static Dispatcher createVKDispatcher(Map<String, MockResponse> responseMap) {
        return new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
                String path = recordedRequest.getPath();

                MockResponse response = responseMap.entrySet()
                        .stream()
                        .filter(entry -> path.startsWith(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(new MockResponse().setResponseCode(404));

                return response;
            }
        };
    }

}