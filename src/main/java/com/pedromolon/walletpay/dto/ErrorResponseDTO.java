package com.pedromolon.walletpay.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ErrorResponseDTO(
        int status,
        String message,
        LocalDateTime timestamp,
        List<String> details
) {
}
