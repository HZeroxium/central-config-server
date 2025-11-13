package com.example.control.infrastructure.cache.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Base64;

/**
 * Custom Jackson deserializer for byte arrays that deserializes Base64 strings back to byte arrays.
 * <p>
 * This works in conjunction with {@link ByteArrayBase64Serializer} to properly
 * handle byte arrays in Redis cache serialization.
 * </p>
 */
public class ByteArrayBase64Deserializer extends JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String base64 = p.getValueAsString();
        if (base64 == null || base64.isEmpty()) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IOException("Failed to decode Base64 string to byte array", e);
        }
    }
}

