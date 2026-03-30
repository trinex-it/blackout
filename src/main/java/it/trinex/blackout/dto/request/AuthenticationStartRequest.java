package it.trinex.blackout.dto.request;

import lombok.Data;

@Data
public class AuthenticationStartRequest {
    private String username; // Optional - can be empty for usernameless flow
}

