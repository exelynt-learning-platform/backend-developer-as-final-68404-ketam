package com.roshan.resourcebooking.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class JwtService {

	private final byte[] secret;
	private final long expirationMs;

	public JwtService(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration-ms:86400000}") long expirationMs) {
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.expirationMs = expirationMs;
		if (this.secret.length < 32) {
			throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256");
		}
	}

	public String generateToken(String username, String role) {
		try {
			Instant now = Instant.now();
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
					.subject(username)
					.claim("role", role)
					.issueTime(Date.from(now))
					.expirationTime(Date.from(now.plusMillis(expirationMs)))
					.build();
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			jwt.sign(new MACSigner(secret));
			return jwt.serialize();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Unable to create JWT", ex);
		}
	}

	public String extractUsername(String token) {
		return parse(token).getSubject();
	}

	public String extractRole(String token) {
		Object role = parse(token).getClaim("role");
		return role == null ? null : role.toString();
	}

	public boolean isValid(String token) {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!jwt.verify(new MACVerifier(secret))) {
				return false;
			}
			Date expiration = jwt.getJWTClaimsSet().getExpirationTime();
			return expiration != null && expiration.after(new Date());
		}
		catch (Exception ex) {
			return false;
		}
	}

	private JWTClaimsSet parse(String token) {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!jwt.verify(new MACVerifier(secret))) {
				throw new IllegalArgumentException("Invalid JWT signature");
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			Date expiration = claims.getExpirationTime();
			if (expiration == null || expiration.before(new Date())) {
				throw new IllegalArgumentException("Expired JWT");
			}
			return claims;
		}
		catch (IllegalArgumentException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Invalid JWT", ex);
		}
	}
}
