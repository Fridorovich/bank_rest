package com.example.bankcards.security;

import com.example.bankcards.dto.JwtDto;
import com.example.bankcards.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtService {

    @Value("8074658237c236e39e96e909ac1abb25a3e1773b100096ad6877c439cd452c17")
    private String jwtSecret;

    public JwtDto generateAuthToken(User user) {
        JwtDto jwtDto = new JwtDto();
        jwtDto.setToken(generateJwtToken(user));
        jwtDto.setRefreshToken(generateRefreshToken(user));
        return jwtDto;
    }

    public JwtDto refreshBaseToken(User user, String refreshToken) {
        JwtDto jwtDto = new JwtDto();
        jwtDto.setToken(generateJwtToken(user));
        jwtDto.setRefreshToken(refreshToken);
        return jwtDto;
    }

    private String generateJwtToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));

        Date expirationDate = Date.from(
                LocalDateTime.now()
                        .plusHours(2)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    private String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");

        Date expirationDate = Date.from(
                LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", Long.class);
    }

    public boolean validateJwtToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (ExpiredJwtException expEx) {
            System.err.println("Expired JWT token: " + expEx.getMessage());
        } catch (UnsupportedJwtException unsEx) {
            System.err.println("Unsupported JWT token: " + unsEx.getMessage());
        } catch (MalformedJwtException malEx) {
            System.err.println("Invalid JWT token: " + malEx.getMessage());
        } catch (SecurityException secEx) {
            System.err.println("Security exception: " + secEx.getMessage());
        } catch (Exception e) {
            System.err.println("Invalid token: " + e.getMessage());
        }
        return false;
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
