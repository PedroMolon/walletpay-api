package com.pedromolon.walletpay.service;

import com.pedromolon.walletpay.TestcontainersConfiguration;
import com.pedromolon.walletpay.dto.DepositRequestDTO;
import com.pedromolon.walletpay.dto.TransferRequestDTO;
import com.pedromolon.walletpay.dto.TransactionResponseDTO;
import com.pedromolon.walletpay.dto.WalletBalancerResponseDTO;
import com.pedromolon.walletpay.exception.BusinessRuleException;
import com.pedromolon.walletpay.exception.ResourceNotFoundException;
import com.pedromolon.walletpay.model.Role;
import com.pedromolon.walletpay.model.Transaction;
import com.pedromolon.walletpay.model.TransactionType;
import com.pedromolon.walletpay.model.User;
import com.pedromolon.walletpay.model.Wallet;
import com.pedromolon.walletpay.repository.RoleRepository;
import com.pedromolon.walletpay.repository.TransactionRepository;
import com.pedromolon.walletpay.repository.UserRepository;
import com.pedromolon.walletpay.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;
    private User userA;
    private User userB;
    private Wallet walletA;
    private Wallet walletB;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        userA = userRepository.save(User.builder()
                .name("User A")
                .email("usera@test.com")
                .password("password")
                .role(userRole)
                .build());

        userB = userRepository.save(User.builder()
                .name("User B")
                .email("userb@test.com")
                .password("password")
                .role(userRole)
                .build());

        walletA = walletRepository.save(Wallet.builder()
                .user(userA)
                .balance(BigDecimal.ZERO)
                .blocked(false)
                .build());

        walletB = walletRepository.save(Wallet.builder()
                .user(userB)
                .balance(BigDecimal.ZERO)
                .blocked(false)
                .build());

        userA.setWallet(walletA);
        userB.setWallet(walletB);
        userRepository.saveAll(List.of(userA, userB));
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should deposit successfully and return updated wallet balance")
        void shouldDepositSuccessfully() {
            DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("100.00"));

            WalletBalancerResponseDTO response = walletService.deposit(request, userA.getId());

            assertThat(response).isNotNull();
            assertThat(response.walletId()).isEqualTo(walletA.getId());
            assertThat(response.userId()).isEqualTo(userA.getId());
            assertThat(response.ownerName()).isEqualTo("User A");
            assertThat(response.balance()).isEqualByComparingTo("100.00");
            assertThat(response.blocked()).isFalse();

            Wallet savedWallet = walletRepository.findById(walletA.getId()).orElseThrow();
            assertThat(savedWallet.getBalance()).isEqualByComparingTo("100.00");

            List<Transaction> transactions = transactionRepository.findAllByWalletId(walletA.getId(), PageRequest.of(0, 10)).getContent();
            assertThat(transactions).hasSize(1);
            assertThat(transactions.getFirst().getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(transactions.getFirst().getAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when deposit amount is zero")
        void shouldThrowWhenDepositAmountIsZero() {
            DepositRequestDTO request = new DepositRequestDTO(BigDecimal.ZERO);

            assertThatThrownBy(() -> walletService.deposit(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Invalid deposit amount");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when deposit amount is negative")
        void shouldThrowWhenDepositAmountIsNegative() {
            DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("-50.00"));

            assertThatThrownBy(() -> walletService.deposit(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Invalid deposit amount");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("100.00"));

            assertThatThrownBy(() -> walletService.deposit(request, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wallet not found");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when wallet is blocked")
        void shouldThrowWhenWalletIsBlocked() {
            walletA.setBlocked(true);
            walletRepository.save(walletA);

            DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("100.00"));

            assertThatThrownBy(() -> walletService.deposit(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Wallet is blocked");
        }

        @Test
        @DisplayName("Should accumulate balance on multiple deposits")
        void shouldAccumulateBalanceOnMultipleDeposits() {
            walletService.deposit(new DepositRequestDTO(new BigDecimal("50.00")), userA.getId());
            walletService.deposit(new DepositRequestDTO(new BigDecimal("75.50")), userA.getId());

            Wallet savedWallet = walletRepository.findById(walletA.getId()).orElseThrow();
            assertThat(savedWallet.getBalance()).isEqualByComparingTo("125.50");
        }
    }

    @Nested
    @DisplayName("Transfer Tests")
    class TransferTests {

        @BeforeEach
        void setUpForTransfer() {
            walletA.setBalance(new BigDecimal("500.00"));
            walletB.setBalance(new BigDecimal("100.00"));
            walletRepository.saveAll(List.of(walletA, walletB));
        }

        @Test
        @DisplayName("Should transfer successfully between wallets")
        void shouldTransferSuccessfully() {
            TransferRequestDTO request = new TransferRequestDTO(userB.getId(), new BigDecimal("150.00"));

            TransactionResponseDTO response = walletService.transfer(request, userA.getId());

            assertThat(response).isNotNull();
            assertThat(response.amount()).isEqualByComparingTo("150.00");
            assertThat(response.type()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(response.senderWalletId()).isEqualTo(walletA.getId());
            assertThat(response.recipientWalletId()).isEqualTo(walletB.getId());

            Wallet updatedSender = walletRepository.findById(walletA.getId()).orElseThrow();
            Wallet updatedRecipient = walletRepository.findById(walletB.getId()).orElseThrow();
            assertThat(updatedSender.getBalance()).isEqualByComparingTo("350.00");
            assertThat(updatedRecipient.getBalance()).isEqualByComparingTo("250.00");

            // Sender sees TRANSFER_OUT, recipient sees TRANSFER_IN
            List<Transaction> senderTransactions = transactionRepository.findAllByWalletId(walletA.getId(), PageRequest.of(0, 10)).getContent();
            List<Transaction> recipientTransactions = transactionRepository.findAllByWalletId(walletB.getId(), PageRequest.of(0, 10)).getContent();
            assertThat(senderTransactions).hasSize(1);
            assertThat(senderTransactions.getFirst().getType()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(recipientTransactions).hasSize(1);
            assertThat(recipientTransactions.getFirst().getType()).isEqualTo(TransactionType.TRANSFER_IN);
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when transfer amount is zero")
        void shouldThrowWhenTransferAmountIsZero() {
            TransferRequestDTO request = new TransferRequestDTO(userB.getId(), BigDecimal.ZERO);

            assertThatThrownBy(() -> walletService.transfer(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Invalid amount");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when transfer amount is negative")
        void shouldThrowWhenTransferAmountIsNegative() {
            TransferRequestDTO request = new TransferRequestDTO(userB.getId(), new BigDecimal("-50.00"));

            assertThatThrownBy(() -> walletService.transfer(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Invalid amount");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when sender and recipient are the same")
        void shouldThrowWhenSenderAndRecipientAreSame() {
            TransferRequestDTO request = new TransferRequestDTO(userA.getId(), new BigDecimal("100.00"));

            assertThatThrownBy(() -> walletService.transfer(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Sender and receiver cannot be the same");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when sender wallet not found")
        void shouldThrowWhenSenderWalletNotFound() {
            TransferRequestDTO request = new TransferRequestDTO(userB.getId(), new BigDecimal("100.00"));

            assertThatThrownBy(() -> walletService.transfer(request, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Sender wallet not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when recipient wallet not found")
        void shouldThrowWhenRecipientWalletNotFound() {
            TransferRequestDTO request = new TransferRequestDTO(999L, new BigDecimal("100.00"));

            assertThatThrownBy(() -> walletService.transfer(request, userA.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Recipient wallet not found");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when sender wallet is blocked")
        void shouldThrowWhenSenderWalletIsBlocked() {
            walletA.setBlocked(true);
            walletRepository.save(walletA);

            TransferRequestDTO request = new TransferRequestDTO(userB.getId(), new BigDecimal("100.00"));

            assertThatThrownBy(() -> walletService.transfer(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Sender wallet is blocked");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when recipient wallet is blocked")
        void shouldThrowWhenRecipientWalletIsBlocked() {
            walletB.setBlocked(true);
            walletRepository.save(walletB);

            TransferRequestDTO request = new TransferRequestDTO(userB.getId(), new BigDecimal("100.00"));

            assertThatThrownBy(() -> walletService.transfer(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Recipient wallet is blocked");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when insufficient balance")
        void shouldThrowWhenInsufficientBalance() {
            TransferRequestDTO request = new TransferRequestDTO(userB.getId(), new BigDecimal("600.00"));

            assertThatThrownBy(() -> walletService.transfer(request, userA.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Insufficient balance");
        }

        @Test
        @DisplayName("Should create both TRANSFER_OUT and TRANSFER_IN transactions")
        void shouldCreateBothTransferTransactions() {
            TransferRequestDTO request = new TransferRequestDTO(userB.getId(), new BigDecimal("100.00"));
            walletService.transfer(request, userA.getId());

            List<Transaction> senderTransactions = transactionRepository.findAllByWalletId(walletA.getId(), PageRequest.of(0, 10)).getContent();
            List<Transaction> recipientTransactions = transactionRepository.findAllByWalletId(walletB.getId(), PageRequest.of(0, 10)).getContent();

            assertThat(senderTransactions).hasSize(1);
            assertThat(senderTransactions.getFirst().getType()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(recipientTransactions).hasSize(1);
            assertThat(recipientTransactions.getFirst().getType()).isEqualTo(TransactionType.TRANSFER_IN);

            assertThat(transactionRepository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Get Balance Tests")
    class GetBalanceTests {

        @Test
        @DisplayName("Should return wallet balance successfully")
        void shouldReturnBalanceSuccessfully() {
            walletA.setBalance(new BigDecimal("250.75"));
            walletRepository.save(walletA);

            BigDecimal balance = walletService.getBalance(userA.getId());

            assertThat(balance).isEqualByComparingTo("250.75");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            assertThatThrownBy(() -> walletService.getBalance(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wallet not found");
        }
    }

    @Nested
    @DisplayName("Get Statement Tests")
    class GetStatementTests {

        @Test
        @DisplayName("Should return paginated statement with transactions")
        void shouldReturnPaginatedStatement() {
            walletA.setBalance(new BigDecimal("500.00"));
            walletRepository.save(walletA);

            walletService.deposit(new DepositRequestDTO(new BigDecimal("100.00")), userA.getId());
            walletService.deposit(new DepositRequestDTO(new BigDecimal("50.00")), userA.getId());

            Pageable pageable = PageRequest.of(0, 10);
            var page = walletService.getStatement(userA.getId(), pageable);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent().getFirst().amount()).isEqualByComparingTo("50.00");
            assertThat(page.getContent().get(1).amount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Should return empty page when no transactions exist")
        void shouldReturnEmptyPageWhenNoTransactions() {
            Pageable pageable = PageRequest.of(0, 10);
            var page = walletService.getStatement(userA.getId(), pageable);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> walletService.getStatement(999L, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wallet not found");
        }

        @Test
        @DisplayName("Should respect pagination")
        void shouldRespectPagination() {
            walletA.setBalance(new BigDecimal("500.00"));
            walletRepository.save(walletA);

            for (int i = 1; i <= 5; i++) {
                walletService.deposit(new DepositRequestDTO(new BigDecimal(i * 10)), userA.getId());
            }

            Pageable pageable = PageRequest.of(0, 2);
            var page = walletService.getStatement(userA.getId(), pageable);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }
    }
}