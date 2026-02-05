package com.leeinx.acasb.jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public class JwtUtils{
    private static final long START_TIME = System.currentTimeMillis();
    private static final String SALT = "ACASB_SPRINGBOOT_SEC";

    private static String get_SecKey(){
        return SALT + '_' + START_TIME;
    }
    public static String createTimestampToken(){
        return Jwts.builder()
            .setSubject("ACASB_ADMIN")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000L * 24 * 30))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(get_SecKey().getBytes()), SignatureAlgorithm.HS256)
            .compact();

    }
    public static boolean validateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(get_SecKey().getBytes())).build().parseClaimsJws(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}