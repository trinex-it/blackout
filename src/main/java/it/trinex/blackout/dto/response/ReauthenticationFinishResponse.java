package it.trinex.blackout.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseCookie;

@Builder
@Getter
public class ReauthenticationFinishResponse {

    private String reauthToken;

    @JsonIgnore
    private ResponseCookie tokenCookie;
}
