package it.trinex.nnh.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard error response for NNH exceptions")
public class ExceptionResponseDTO {

    @Schema(description = "Timestamp when the exception occurred", example = "2025-01-30T10:00:00Z")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "401")
    private Integer status;

    @Schema(description = "Error category for programmatic handling", example = "ACCOUNT_NOT_ACTIVE")
    private String category;

    @Schema(description = "Human-readable error message", example = "Account is not active")
    private String message;

    public ExceptionResponseDTO(NNHException e) {
        this.timestamp = Instant.now();
        this.status = e.getStatus().value();
        this.category = e.getCategory();
        this.message = e.getDescription();
    }
}