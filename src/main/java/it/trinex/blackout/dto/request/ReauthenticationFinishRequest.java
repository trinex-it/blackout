package it.trinex.blackout.dto.request;

import lombok.Data;

@Data
public class ReauthenticationFinishRequest {
    private String id;
    private String rawId;
    private String type;
    private Response response;
    
    @Data
    public static class Response {
        private String clientDataJSON;
        private String authenticatorData;
        private String signature;
        private String userHandle;
    }
}

