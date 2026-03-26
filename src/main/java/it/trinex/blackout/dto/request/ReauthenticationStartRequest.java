package it.trinex.blackout.dto.request;

import lombok.Data;

@Data
public class ReauthenticationStartRequest {
    private String reason; // Optional: reason for reauthentication
}

