package com.leeinx.acasb.jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class JwtUtils{
    private static final long START_TIME = System.currentTimeMillis();
    private static volatile String secretKey = buildDefaultSecret();
    private static volatile long expiresHours = 24L * 30;

    private static String buildDefaultSecret() {
        return "ACASB_SPRINGBOOT_SEC_" + START_TIME + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static synchronized void configure(String configuredSecret, long configuredExpiresHours){
        if (configuredSecret != null && !configuredSecret.trim().isEmpty()) {
            secretKey = configuredSecret.trim();
        }
        if (configuredExpiresHours > 0) {
            expiresHours = configuredExpiresHours;
        }
    }

    private static byte[] signingKeyBytes() {
        return secretKey.getBytes(StandardCharsets.UTF_8);
    }

    public static String createTimestampToken(){
        return Jwts.builder()
            .setSubject("ACASB_ADMIN")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000L * expiresHours))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(signingKeyBytes()), SignatureAlgorithm.HS256)
            .compact();

    }
    public static boolean validateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(signingKeyBytes())).build().parseClaimsJws(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
