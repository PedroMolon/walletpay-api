package com.pedromolon.walletpay.service;

import com.pedromolon.walletpay.dto.DepositRequestDTO;
import com.pedromolon.walletpay.dto.TransactionResponseDTO;
import com.pedromolon.walletpay.dto.TransferRequestDTO;
import com.pedromolon.walletpay.dto.WalletBalancerResponseDTO;
import com.pedromolon.walletpay.exception.BusinessRuleException;
import com.pedromolon.walletpay.exception.ResourceNotFoundException;
import com.pedromolon.walletpay.model.Transaction;
import com.pedromolon.walletpay.model.TransactionType;
import com.pedromolon.walletpay.model.Wallet;
import com.pedromolon.walletpay.repository.TransactionRepository;
import com.pedromolon.walletpay.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WalletBalancerResponseDTO deposit(DepositRequestDTO request, Long userId) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Invalid deposit amount");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (Boolean.TRUE.equals(wallet.getBlocked())) {
            throw new BusinessRuleException("Wallet is blocked");
        }

        wallet.setBalance(wallet.getBalance().add(request.amount()));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .amount(request.amount())
                .type(TransactionType.DEPOSIT)
                .senderWallet(null)
                .recipientWallet(wallet)
                .build();

        transactionRepository.save(transaction);

        return WalletBalancerResponseDTO.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUser().getId())
                .ownerName(wallet.getUser().getName())
                .balance(wallet.getBalance())
                .blocked(wallet.getBlocked())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Transactional
    public TransactionResponseDTO transfer(TransferRequestDTO request, Long senderId) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Invalid amount");
        }

        if (senderId.equals(request.recipientId())) {
            throw new BusinessRuleException("Sender and receiver cannot be the same");
        }

        Wallet senderWallet = walletRepository.findByUserId(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        Wallet recipientWallet = walletRepository.findByUserId(request.recipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient wallet not found"));

        if (Boolean.TRUE.equals(senderWallet.getBlocked())) {
            throw new BusinessRuleException("Sender wallet is blocked");
        }

        if (Boolean.TRUE.equals(recipientWallet.getBlocked())) {
            throw new BusinessRuleException("Recipient wallet is blocked");
        }

        if (senderWallet.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessRuleException("Insufficient balance");
        }

        senderWallet.setBalance(senderWallet.getBalance().subtract(request.amount()));
        recipientWallet.setBalance(recipientWallet.getBalance().add(request.amount()));

        walletRepository.saveAll(List.of(senderWallet, recipientWallet));

        Transaction outTransaction = Transaction.builder()
                .amount(request.amount())
                .type(TransactionType.TRANSFER_OUT)
                .senderWallet(senderWallet)
                .recipientWallet(recipientWallet)
                .build();

        Transaction inTransaction = Transaction.builder()
                .amount(request.amount())
                .type(TransactionType.TRANSFER_IN)
                .senderWallet(senderWallet)
                .recipientWallet(recipientWallet)
                .build();

        transactionRepository.saveAll(List.of(outTransaction, inTransaction));

        return TransactionResponseDTO.builder()
                .id(outTransaction.getId())
                .amount(outTransaction.getAmount())
                .type(outTransaction.getType())
                .senderWalletId(outTransaction.getSenderWallet().getId())
                .recipientWalletId(outTransaction.getRecipientWallet().getId())
                .createdAt(outTransaction.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        return wallet.getBalance();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getStatement(Long userId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        return transactionRepository.findAllByWalletId(wallet.getId(), pageable)
                .map(transaction -> TransactionResponseDTO.builder()
                        .id(transaction.getId())
                        .amount(transaction.getAmount())
                        .type(transaction.getType())
                        .senderWalletId(transaction.getSenderWallet() != null ? transaction.getSenderWallet().getId() : null)
                        .recipientWalletId(transaction.getRecipientWallet() != null ? transaction.getRecipientWallet().getId() : null)
                        .createdAt(transaction.getCreatedAt())
                        .build());
    }

}
