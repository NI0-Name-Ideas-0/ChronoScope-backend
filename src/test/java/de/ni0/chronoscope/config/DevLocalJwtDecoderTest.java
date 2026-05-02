package de.ni0.chronoscope.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class DevLocalJwtDecoderTest {

    @Test
    void decode_ReturnsLocalJwtForConfiguredDevToken() {
        JwtDecoder delegate = mock(JwtDecoder.class);
        DevAuthProperties properties = new DevAuthProperties(
            " dev-token ",
            " local-subject ",
            List.of(" private ", "chronoscope-local"),
            List.of(" /org-admin/chronoscope-local ")
        );
        DevLocalJwtDecoder decoder = new DevLocalJwtDecoder(properties, () -> delegate);

        Jwt jwt = decoder.decode("dev-token");

        assertEquals("local-subject", jwt.getSubject());
        assertEquals(List.of("private", "chronoscope-local"), jwt.getClaimAsStringList("organization"));
        assertEquals(List.of("/org-admin/chronoscope-local"), jwt.getClaimAsStringList("groups"));
        verifyNoInteractions(delegate);
    }

    @Test
    void decode_DelegatesNonDevTokens() {
        Jwt expectedJwt = Jwt.withTokenValue("real-token")
            .header("alg", "none")
            .claim("sub", "real-subject")
            .build();
        JwtDecoder delegate = mock(JwtDecoder.class);
        when(delegate.decode("real-token")).thenReturn(expectedJwt);
        DevLocalJwtDecoder decoder = new DevLocalJwtDecoder(
            new DevAuthProperties("dev-token", "local-subject", List.of("private")),
            () -> delegate
        );

        Jwt actualJwt = decoder.decode("real-token");

        assertSame(expectedJwt, actualJwt);
        verify(delegate).decode("real-token");
    }

    @Test
    void devAuthConfig_IsOnlyActiveForDevAndNotProd() {
        Profile profile = DevAuthConfig.class.getAnnotation(Profile.class);

        assertArrayEquals(new String[] { "dev & !prod" }, profile.value());
    }
}
