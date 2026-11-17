package com.pedromolon.walletpay.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record WalletBalancerResponseDTO(
        Long walletId,
        Long userId,
        String ownerName,
        BigDecimal balance,
        Boolean blocked,
        LocalDateTime updatedAt
) {
}
