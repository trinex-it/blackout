package it.trinex.blackout.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ResetPasswordOTPRequest {
    @NotBlank(message = "OTP is required.")
    private String otp;
    @NotBlank(message = "New password is required")
    private String newPassword;

    private String passwordRepeat;

    @AssertTrue(message = "Passwords do not match.")
    @JsonIgnore
    public boolean isPasswordsMatching() {
        if (newPassword == null || passwordRepeat == null) {
            return true; // Skip validation when fields are null, let @NotNull handle it
        }
        return newPassword.equals(passwordRepeat);
    }
}
