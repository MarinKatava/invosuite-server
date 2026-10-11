package com.invoisuite.server.integration;

import com.invoisuite.server.common.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutboundUrlValidatorTest {

    private OutboundUrlValidator validator(boolean allowPrivate, boolean allowHttp) {
        IntegrationProperties p = new IntegrationProperties();
        p.setAllowPrivateHosts(allowPrivate);
        p.setAllowInsecureHttp(allowHttp);
        return new OutboundUrlValidator(p);
    }

    @Test
    void rejectsInternalAddressesByDefault() {
        OutboundUrlValidator v = validator(false, false);
        for (String url : new String[]{
                "https://127.0.0.1", "https://localhost", "https://10.1.2.3", "https://192.168.0.5",
                "https://172.16.0.1", "https://169.254.169.254/latest/meta-data", "https://0.0.0.0",
                "https://100.64.0.1", "https://[::1]", "https://[fd00::1]", "https://[fe80::1]"}) {
            assertThrows(BadRequestException.class, () -> v.validate(url), url);
        }
    }

    @Test
    void rejectsPlainHttpUnlessAllowed() {
        assertThrows(BadRequestException.class, () -> validator(false, false).validate("http://8.8.8.8"));
        assertEquals("http://8.8.8.8", validator(false, true).validate("http://8.8.8.8"));
    }

    @Test
    void rejectsCredentialsQueryAndGarbage() {
        OutboundUrlValidator v = validator(true, true);
        assertThrows(BadRequestException.class, () -> v.validate("https://user:pw@example.com"));
        assertThrows(BadRequestException.class, () -> v.validate("https://example.com/?x=1"));
        assertThrows(BadRequestException.class, () -> v.validate("not a url"));
        assertThrows(BadRequestException.class, () -> v.validate("ftp://example.com"));
        assertThrows(BadRequestException.class, () -> v.validate(""));
    }

    @Test
    void normalisesTrailingSlashAndAllowsPublicAddresses() {
        OutboundUrlValidator v = validator(false, false);
        assertEquals("https://8.8.8.8", v.validate("https://8.8.8.8/"));
        assertEquals("https://8.8.4.4:8443/odoo", v.validate(" https://8.8.4.4:8443/odoo/ "));
    }

    @Test
    void privateHostsAllowedWhenExplicitlyEnabled() {
        assertEquals("http://127.0.0.1:8069", validator(true, true).validate("http://127.0.0.1:8069"));
    }
}
