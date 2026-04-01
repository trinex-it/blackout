package it.trinex.blackout.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.ResponseStatus;

@Builder
@Getter
public class RegistrationFinishResponse {
    String accessToken;
    @JsonIgnore
    ResponseCookie accessTokenCookie;
}
