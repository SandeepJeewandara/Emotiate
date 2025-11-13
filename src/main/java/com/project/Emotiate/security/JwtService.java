package com.project.Emotiate.security;

import com.project.Emotiate.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // Method to generate JWT token for a user
    public String generateToken(User user) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }


    // Helper Method to extract username from JWT token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    // Helper Method to extract user role from JWT token
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }


    // Helper Method to validate the JWT token
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }


    // Helper Method to validate the JWT token
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }


    // Helper Method to extract specific claim from JWT token
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }


    // Helper Method to extract all claims from JWT token
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    // Helper Method to validate the JWT token
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}