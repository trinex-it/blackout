package it.trinex.blackout.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationStartResponse {
    
    private String challenge;
    private Long timeout;
    private String rpId;
    private List<AllowCredential> allowCredentials;
    private String userVerification;

    @JsonIgnore
    private String sessionId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllowCredential {
        private String type;
        private String id;
        private List<String> transports;
    }
}

