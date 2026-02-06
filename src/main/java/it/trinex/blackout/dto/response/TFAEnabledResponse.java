package it.trinex.blackout.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class TFAEnabledResponse {
    private List<String> recoveryCodes;
}
