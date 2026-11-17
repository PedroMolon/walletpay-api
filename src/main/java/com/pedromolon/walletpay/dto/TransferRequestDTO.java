package com.pedromolon.walletpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequestDTO(
        @NotNull(message = "Sender ID is required")
        Long senderId,

        @NotNull(message = "Recipient ID is required")
        Long recipientId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than or equal to 0.01")
        BigDecimal amount
) {
}
