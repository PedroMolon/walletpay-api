package com.pedromolon.walletpay.dto;

import lombok.Builder;

@Builder
public record RegisterResponseDTO(
        Long id,
        String name,
        String email
) {
}
