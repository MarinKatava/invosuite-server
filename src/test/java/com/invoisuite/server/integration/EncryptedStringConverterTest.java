package com.invoisuite.server.integration;

import com.invoisuite.server.security.EncryptedStringConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedStringConverterTest {

    @Test
    void roundTripsAndDoesNotStorePlaintext() {
        EncryptedStringConverter c = new EncryptedStringConverter("some-key");
        String stored = c.convertToDatabaseColumn("my-odoo-api-key");
        assertNotEquals("my-odoo-api-key", stored);
        assertFalse(stored.contains("odoo"));
        assertEquals("my-odoo-api-key", c.convertToEntityAttribute(stored));
    }

    @Test
    void usesFreshIvEachTime() {
        EncryptedStringConverter c = new EncryptedStringConverter("some-key");
        assertNotEquals(c.convertToDatabaseColumn("same"), c.convertToDatabaseColumn("same"));
    }

    @Test
    void wrongKeyOrTamperingFailsLoudly() {
        String stored = new EncryptedStringConverter("key-a").convertToDatabaseColumn("secret");
        assertThrows(IllegalStateException.class, () -> new EncryptedStringConverter("key-b").convertToEntityAttribute(stored));
        assertThrows(IllegalStateException.class, () -> new EncryptedStringConverter("key-a").convertToEntityAttribute("not-base64!!"));
    }

    @Test
    void nullsPassThrough() {
        EncryptedStringConverter c = new EncryptedStringConverter("k");
        assertNull(c.convertToDatabaseColumn(null));
        assertNull(c.convertToEntityAttribute(null));
    }
}
