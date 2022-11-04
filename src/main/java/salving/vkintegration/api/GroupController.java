package salving.vkintegration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import salving.vkintegration.service.VKService;
import salving.vkintegration.domain.VKUser;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final VKService vkService;

    @Cacheable(value = "users", key = "#groupCheckRequest")
    @Operation(summary = "Check user membership")
    @GetMapping(value = "/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<GroupCheckResponse> check(
            @RequestBody @Valid GroupCheckRequest groupCheckRequest,
            @RequestHeader("vk_service_token") @NotBlank String vkToken) {
        Mono<VKUser> userMono = vkService.getUser(groupCheckRequest.userId, vkToken);
        Mono<Boolean> memberMono = vkService.isMember(groupCheckRequest.userId, groupCheckRequest.groupId, vkToken);

        return userMono.zipWith(memberMono, GroupCheckResponse::new);
    }

    record GroupCheckRequest
            (@JsonProperty("user_id") @NotBlank @Size(min = 1, max = 32) String userId,
             @JsonProperty("group_id") @NotBlank @Size(min = 1, max = 32) String groupId) {}

    record GroupCheckResponse(@JsonProperty("last_name") String lastName, @JsonProperty("first_name") String firstName,
                              @JsonProperty("middle_name") String middleName, boolean member) {
        public GroupCheckResponse(VKUser user, boolean member) {
            this(user.lastName(), user.firstName(), user.nickname(), member);
        }
    }
}
