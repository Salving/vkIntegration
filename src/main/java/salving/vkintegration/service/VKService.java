package salving.vkintegration.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import salving.vkintegration.domain.VKUser;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Log
@Service
public class VKService {
    private final WebClient webClient;

    String baseUrl;

    public VKService(@Value("${vk.api.url}") String vkApiUrl) {
        this.webClient = WebClient.create(vkApiUrl);
        baseUrl = vkApiUrl;
    }

    private UriBuilder defaultParams(UriBuilder builder, String vkToken) {
        return builder.queryParam("access_token", vkToken)
                .queryParam("v", "5.131")
                .queryParam("lang", "0");
    }

    public Mono<VKUser> getUser(String userId, String vkToken) {
        log.log(Level.INFO, "Fetching user: {0}", userId);

        return webClient.get()
                .uri(uriBuilder -> defaultParams(uriBuilder, vkToken).path("/users.get")
                        .queryParam("user_ids", userId)
                        .queryParam("field", "nickname")
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<VKResponse<List<VKUser>>>() {})
                .flatMapMany(vkResponse -> Flux.fromIterable(vkResponse.response))
                .switchIfEmpty(Flux.error(new UserNotFoundException(userId)))
                .next();
    }

    public Mono<Boolean> isMember(String userId, String groupId, String vkToken) {
        log.log(Level.INFO, "Checking membership of user {0} in group {1}", new String[]{userId, groupId});

        return webClient.get()
                .uri(uriBuilder -> defaultParams(uriBuilder, vkToken).path("/groups.isMember")
                        .queryParam("user_id", userId)
                        .queryParam("group_id", groupId)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    if (jsonNode.has("error")) {
                        String message = jsonNode.get("error").get("error_msg").asText();
                        throw new InvalidParametersException(message);
                    }
                    return jsonNode.get("response").asInt();
                })

//                .bodyToMono(new ParameterizedTypeReference<VKResponse<Integer>>() {})
                .map(response -> response == 1);
    }

    public record VKResponse<T>(T response) {
//        public record Error(@JsonProperty("error_code") int code, @JsonProperty("error_msg") String message,
//                            @JsonProperty("request_params") Map<String, String> requestParams) {}
    }
}
