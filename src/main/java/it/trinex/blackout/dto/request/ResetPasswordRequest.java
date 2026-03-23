package it.trinex.blackout.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "New password is required.")
    private String newPassword;
    @NotBlank(message = "Repeat password is required.")
    private String repeatNewPassword;
    @NotBlank(message = "Old password is required.")
    private String oldPassword;

    @AssertTrue(message = "New password and repeat new password must match")
    @JsonIgnore
    public boolean isPasswordsMatching() {
        if (newPassword == null || repeatNewPassword == null) {
            return true; // Skip validation when fields are null, let @NotNull handle it
        }
        return newPassword.equals(repeatNewPassword);
    }
}
