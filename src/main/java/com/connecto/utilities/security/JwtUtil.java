package com.connecto.utilities.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    //private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256); // Generates a random secret key

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Method to create a JWT
    public String generateToken(String userId, Map<String, Object> claims) {
        return generateToken(userId, claims, 1000L * 60 * 15);
    }

    public String generateRefreshToken(String userId) {
        return generateToken(userId, Map.of("type", "refresh"), 1000L * 60 * 60 * 24 * 7);
    }

    private String generateToken(String userId, Map<String, Object> claims, long ttlMillis) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Method to extract the userId from the JWT
    public String extractUserId(String token) {
        {
            try {
                return Jwts.parser()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject();
            } catch (ExpiredJwtException |UnsupportedJwtException | MalformedJwtException |SecurityException | IllegalArgumentException e) {
                return null;
            }
        }
    }

    // Method to validate the JWT
    public boolean validateToken(String token, String userId) {
        final String extractedUserId = extractUserId(token);
        return extractedUserId != null && extractedUserId.equals(userId) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(parseClaims(token).get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(parseClaims(token).get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Check if the token has expired
    private boolean isTokenExpired(String token) {
        return parseClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
