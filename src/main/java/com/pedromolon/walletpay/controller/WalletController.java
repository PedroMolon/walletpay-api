package com.pedromolon.walletpay.controller;

import com.pedromolon.walletpay.dto.DepositRequestDTO;
import com.pedromolon.walletpay.dto.TransactionResponseDTO;
import com.pedromolon.walletpay.dto.TransferRequestDTO;
import com.pedromolon.walletpay.dto.WalletBalancerResponseDTO;
import com.pedromolon.walletpay.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BigDecimal> getBalance(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = ((Number) Objects.requireNonNull(jwt.getClaim("userId"))).longValue();
        return ResponseEntity.status(HttpStatus.OK).body(walletService.getBalance(userId));
    }

    @GetMapping("/statement")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<TransactionResponseDTO>> getStatement(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = ((Number) Objects.requireNonNull(jwt.getClaim("userId"))).longValue();
        return ResponseEntity.status(HttpStatus.OK).body(walletService.getStatement(userId, pageable));
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<WalletBalancerResponseDTO> deposit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid DepositRequestDTO depositRequestDTO
    ) {
        Long userId = ((Number) Objects.requireNonNull(jwt.getClaim("userId"))).longValue();
        return ResponseEntity.status(HttpStatus.OK).body(walletService.deposit(depositRequestDTO, userId));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TransactionResponseDTO> transfer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid TransferRequestDTO transferRequestDTO
    ) {
        Long senderId = ((Number) Objects.requireNonNull(jwt.getClaim("userId"))).longValue();
        return ResponseEntity.ok(walletService.transfer(transferRequestDTO, senderId));
    }

}
