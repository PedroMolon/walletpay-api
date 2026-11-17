package com.pedromolon.walletpay.security;

import com.pedromolon.walletpay.dto.LoginRequestDTO;
import com.pedromolon.walletpay.dto.LoginResponseDTO;
import com.pedromolon.walletpay.dto.RegisterRequestDTO;
import com.pedromolon.walletpay.dto.RegisterResponseDTO;
import com.pedromolon.walletpay.model.Role;
import com.pedromolon.walletpay.model.User;
import com.pedromolon.walletpay.repository.RoleRepository;
import com.pedromolon.walletpay.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        Optional<Role> role = roleRepository.findByName("ROLE_USER");

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(role.orElse(null))
                .build();

        userRepository.save(user);

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
