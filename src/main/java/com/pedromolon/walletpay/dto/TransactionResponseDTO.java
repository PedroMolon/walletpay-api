package com.pedromolon.walletpay.dto;

import com.pedromolon.walletpay.model.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionResponseDTO(
        Long id,
        TransactionType type,
        BigDecimal amount,
        Long senderWalletId,
        Long recipientWalletId,
        LocalDateTime createdAt
) {
}
