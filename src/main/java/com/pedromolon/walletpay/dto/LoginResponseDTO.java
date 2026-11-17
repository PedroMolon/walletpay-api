package com.pedromolon.walletpay.dto;

import lombok.Builder;

@Builder
public record LoginResponseDTO(
        String token,
        Long expiresIn
) {
}
