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

/**
 * Сервис для работы с JWT токенами
 * Обеспечивает генерацию, валидацию и извлечение данных из токенов
 */
@Component
public class JwtService {

    //@Value("${app.jwt.secret}")
    @Value("8074658237c236e39e96e909ac1abb25a3e1773b100096ad6877c439cd452c17")
    private String jwtSecret;

    /**
     * Генерирует пару JWT токенов для пользователя
     * Создает access token и refresh token с разными сроками действия
     *
     * @param user объект пользователя для которого генерируются токены
     * @return DTO объект содержащий оба токена
     */
    public JwtDto generateAuthToken(User user) {
        JwtDto jwtDto = new JwtDto();
        jwtDto.setToken(generateJwtToken(user));
        jwtDto.setRefreshToken(generateRefreshToken(user));
        return jwtDto;
    }

    /**
     * Обновляет access token используя существующий refresh token
     * Генерирует новый access token но сохраняет переданный refresh token
     *
     * @param user объект пользователя
     * @param refreshToken валидный refresh token
     * @return DTO объект с новым access token и существующим refresh token
     */
    public JwtDto refreshBaseToken(User user, String refreshToken) {
        JwtDto jwtDto = new JwtDto();
        jwtDto.setToken(generateJwtToken(user));
        jwtDto.setRefreshToken(refreshToken);
        return jwtDto;
    }

    /**
     * Генерирует access token с коротким сроком действия
     * Включает в payload идентификатор пользователя и его роли
     *
     * @param user объект пользователя
     * @return сгенерированный JWT access token
     */
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

    /**
     * Генерирует refresh token с длительным сроком действия
     * Используется для получения новых access token без повторной аутентификации
     *
     * @param user объект пользователя
     * @return сгенерированный JWT refresh token
     */
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

    /**
     * Извлекает имя пользователя из JWT токена
     *
     * @param token JWT токен
     * @return имя пользователя (subject токена)
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Извлекает идентификатор пользователя из JWT токена
     *
     * @param token JWT токен
     * @return идентификатор пользователя
     */
    public Long getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", Long.class);
    }

    /**
     * Проверяет валидность JWT токена
     * Выполняет проверку подписи, срока действия и структуры токена
     *
     * @param token JWT токен для проверки
     * @return true если токен валиден, false в противном случае
     */
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

    /**
     * Извлекает claims (утверждения) из JWT токена
     *
     * @param token JWT токен
     * @return объект Claims содержащий данные из payload токена
     * @throws JwtException если токен невалиден
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Создает секретный ключ для подписи JWT токенов
     * Использует HMAC-SHA алгоритм с ключом из application properties
     *
     * @return секретный ключ для подписи
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}