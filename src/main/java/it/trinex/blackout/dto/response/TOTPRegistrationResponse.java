package it.trinex.blackout.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TOTPRegistrationResponse {
    private String secret;
    private String qrURI;
}
