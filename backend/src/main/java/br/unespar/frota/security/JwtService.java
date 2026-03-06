package br.unespar.frota.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${frota.jwt.secret}")
    private String secret;

    private Key key() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HMAC-SHA256 requer no mínimo 32 bytes
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 caracteres");
        return Keys.hmacShaKeyFor(bytes);
    }

    public String gerarToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86_400_000L * 7)) // 7 dias
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String validarToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
