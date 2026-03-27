package com.shopflow.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtHelper {

    @Value("${shopflow.jwt.secret}")
    private String rawSecret;

    @Value("${shopflow.jwt.expiry-ms}")
    private long expiryMs;

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(rawSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email) {
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + expiryMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String tokenEmail = extractEmail(token);
            return tokenEmail.equals(userDetails.getUsername()) && !isExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
