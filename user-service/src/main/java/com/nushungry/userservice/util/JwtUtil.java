package com.nushungry.userservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKeyForNUSHungryApplicationThatIsLongEnoughForHS256Algorithm}")
    private String secret;

    @Value("${jwt.access-token.expiration:3600000}") // 1 hour in milliseconds
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:2592000000}") // 30 days in milliseconds
    private Long refreshTokenExpiration;

    // 保持向后兼容
    @Value("${jwt.expiration:3600000}") // 1 hour (deprecated, use access-token.expiration)
    private Long expiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Removed duplicate private isTokenExpired - using public version at line 233

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createAccessToken(claims, userDetails.getUsername());
    }

    /**
     * Generate Access Token with custom claims (short-lived, 1 hour)
     */
    public String generateAccessToken(String username, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>(additionalClaims);
        return createAccessToken(claims, username);
    }

    /**
     * Generate Refresh Token (long-lived, 30 days)
     * Includes a unique JWT ID (jti) to ensure each token is unique
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString());  // Add unique JWT ID
        return createRefreshToken(claims, username);
    }

    private String createAccessToken(Map<String, Object> claims, String subject) {
        // 注意：必须先设置 subject 等标准 claims，再设置自定义 claims
        // 因为 setClaims() 会清空之前所有的 claims
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .addClaims(claims)  // 使用 addClaims() 而不是 setClaims()
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String createRefreshToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .addClaims(claims)  // 使用 addClaims() 而不是 setClaims()
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // 保持向后兼容
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .addClaims(claims)  // 使用 addClaims() 而不是 setClaims()
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Generate token with custom claims
     * Used for including additional information like roles in the token
     */
    public String generateTokenWithClaims(String username, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>(additionalClaims);
        return createToken(claims, username);
    }

    /**
     * Validate token with username string (for cases where UserDetails is not available)
     */
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * Get JWT expiration time in milliseconds
     */
    public Long getJwtExpiration() {
        return expiration;
    }

    /**
     * Get Access Token expiration time in milliseconds
     */
    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Get Refresh Token expiration time in milliseconds
     */
    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * Extract a custom claim from the token
     */
    public Object extractCustomClaim(String token, String claimName) {
        final Claims claims = extractAllClaims(token);
        return claims.get(claimName);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        final Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else if (userIdObj instanceof String) {
            return Long.parseLong((String) userIdObj);
        }
        return null;
    }

    /**
     * Validate token without UserDetails (just check if token is valid and not expired)
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate token with custom claims (compatible with admin-service)
     * Used by AdminAuthService
     */
    public String generateToken(String username, Map<String, Object> claims) {
        return createAccessToken(claims, username);
    }

    /**
     * Get expiration time (compatible with admin-service)
     */
    public Long getExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Get username from token (compatible with admin-service)
     */
    public String getUsernameFromToken(String token) {
        return extractUsername(token);
    }

    /**
     * Get user ID from token (compatible with admin-service)
     */
    public Long getUserIdFromToken(String token) {
        return extractUserId(token);
    }

    /**
     * Get role from token (compatible with admin-service)
     */
    public String getRoleFromToken(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Check if token is expired (public method for admin-service compatibility)
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}