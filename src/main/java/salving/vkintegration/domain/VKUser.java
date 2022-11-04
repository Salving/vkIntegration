package salving.vkintegration.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VKUser(@JsonProperty("last_name") String lastName,
                     @JsonProperty("first_name") String firstName,
                     String nickname) {
}
