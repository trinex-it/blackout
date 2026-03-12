package it.trinex.blackout.controller;

import it.trinex.blackout.dto.request.LoginRequestDTO;
import it.trinex.blackout.dto.request.RefreshRequestDTO;
import org.springframework.http.ResponseEntity;

public interface AuthController {
    ResponseEntity<?> login(LoginRequestDTO request);

    ResponseEntity<?> refresh(RefreshRequestDTO request);

    ResponseEntity<?> getAuthStatus();
}
