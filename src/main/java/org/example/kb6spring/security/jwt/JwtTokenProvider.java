package org.example.kb6spring.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {
    private final String TOKEN_SECRET = "tetz-secret-token-key-for-make-jwt-token";
    private final long TOKEN_VALIDITY = 1000 * 60 * 60;

    public String createToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", roles);
        Date now = new Date();
        Date validity = new Date(now.getTime() + TOKEN_VALIDITY);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SignatureAlgorithm.HS256, TOKEN_SECRET.getBytes())
                .compact();
    }

    public Authentication getAuthentication(String token) {
        String username = getUsername(token);
        return new UsernamePasswordAuthenticationToken(
                username, "",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
    }

    public String getUsername(String token) {
        return Jwts.parser()
                .setSigningKey(TOKEN_SECRET.getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(TOKEN_SECRET.getBytes()).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
