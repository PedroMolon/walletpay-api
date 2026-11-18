package com.pedromolon.walletpay.security;

import com.pedromolon.walletpay.dto.LoginRequestDTO;
import com.pedromolon.walletpay.dto.LoginResponseDTO;
import com.pedromolon.walletpay.dto.RegisterRequestDTO;
import com.pedromolon.walletpay.dto.RegisterResponseDTO;
import com.pedromolon.walletpay.exception.BusinessRuleException;
import com.pedromolon.walletpay.exception.ResourceNotFoundException;
import com.pedromolon.walletpay.model.Role;
import com.pedromolon.walletpay.model.User;
import com.pedromolon.walletpay.model.Wallet;
import com.pedromolon.walletpay.repository.RoleRepository;
import com.pedromolon.walletpay.repository.UserRepository;
import com.pedromolon.walletpay.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final WalletRepository walletRepository;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder, TokenService tokenService, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Email already exists");
        }

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with this name"));

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .build();

        userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(new BigDecimal(BigInteger.ZERO))
                .blocked(false)
                .build();

        walletRepository.save(wallet);

        user.setWallet(wallet);

        return RegisterResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO requestDTO) {
        User user = userRepository.findByEmail(requestDTO.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(requestDTO.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return LoginResponseDTO.builder()
                .token(tokenService.generateToken(user))
                .expiresIn(3600L)
                .build();
    }

}
