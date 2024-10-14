
package com.fintech.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secretKey;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshExpiration;

    public long getExpirationTime() { return jwtExpiration; }

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public String issueToken(UserDetails userDetails) { return createToken(new HashMap<>(), userDetails, jwtExpiration); }

    public String issueToken(Map<String, Object> extraClaims, UserDetails userDetails) { return createToken(extraClaims, userDetails, jwtExpiration); }

    public String issueRefreshToken(UserDetails userDetails) { return createToken(new HashMap<>(), userDetails, refreshExpiration); }

    private String createToken(Map<String, Object> claims, UserDetails userDetails, long expiration) {
        return Jwts.builder().claims(claims).subject(userDetails.getUsername())
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(signingKey(), Jwts.SIG.HS384).compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) { return extractClaim(token, Claims::getExpiration).before(new Date()); }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey signingKey() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)); }

}
