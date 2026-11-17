package com.pedromolon.walletpay.security;

import com.pedromolon.walletpay.model.Role;
import com.pedromolon.walletpay.model.User;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;

    public TokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        long expiresIn = 3600L;

        Role role = user.getRole();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("minha-api")
                .subject(user.getName())
                .claim("userId", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", role.getName())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

}
