package br.unespar.frota.security;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;

@Service
public class MicrosoftTokenService {

    @Value("${frota.azure.tenant-id}")
    private String tenantId;

    @Value("${frota.azure.client-id}")
    private String clientId;

    public String validarEObterEmail(String idToken) throws Exception {
        String jwksUri = "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";

        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwksUri));

        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource)
        );
        // Desabilita validação de claims built-in (faremos manualmente)
        processor.setJWTClaimsSetVerifier((claims, ctx) -> {});

        JWTClaimsSet claims = processor.process(idToken, null);

        // Valida audience
        if (!claims.getAudience().contains(clientId)) {
            throw new SecurityException("Token com audience inválida");
        }

        // Valida expiração
        if (claims.getExpirationTime() == null || claims.getExpirationTime().getTime() < System.currentTimeMillis()) {
            throw new SecurityException("Token expirado");
        }

        String email = claims.getStringClaim("preferred_username");
        if (email == null || email.isBlank()) {
            email = claims.getStringClaim("email");
        }
        if (email == null || email.isBlank()) {
            throw new SecurityException("E-mail não encontrado no token");
        }
        return email.toLowerCase().trim();
    }
}
