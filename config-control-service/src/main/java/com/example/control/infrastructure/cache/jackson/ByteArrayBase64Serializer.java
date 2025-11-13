package com.example.control.infrastructure.cache.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Base64;

/**
 * Custom Jackson serializer for byte arrays that serializes them as Base64 strings.
 * <p>
 * This avoids type information issues with Jackson's default typing when caching
 * objects containing byte arrays in Redis.
 * </p>
 */
public class ByteArrayBase64Serializer extends JsonSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.length == 0) {
            gen.writeString("");
        } else {
            String base64 = Base64.getEncoder().encodeToString(value);
            gen.writeString(base64);
        }
    }
}

