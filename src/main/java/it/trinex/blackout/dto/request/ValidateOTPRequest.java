package it.trinex.blackout.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ValidateOTPRequest {
    @NotBlank(message = "OTP cannot be blank.")
    private String otp;
}
