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
public class RegistrationStartResponse {
    
    private String challenge;
    private RpInfo rp;
    private UserInfo user;
    private List<PublicKeyCredentialParameters> pubKeyCredParams;
    private Long timeout;
    private AuthenticatorSelection authenticatorSelection;
    private String attestation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RpInfo {
        private String name;
        private String id;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String name;
        private String displayName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicKeyCredentialParameters {
        private String type;
        private Integer alg;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticatorSelection {
        private String authenticatorAttachment;
        private String residentKey;
        private Boolean requireResidentKey;
        private String userVerification;
    }
}

