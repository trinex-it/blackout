package it.trinex.blackout.dto.request;

import lombok.Data;

@Data
public class RegistrationFinishRequest {
    
    private String id;
    private String rawId;
    private String type;
    private Response response;
    private String deviceName;
    
    @Data
    public static class Response {
        private String clientDataJSON;
        private String attestationObject;
        private String[] transports;
    }
}

