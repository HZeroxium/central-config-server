package com.example.control.infrastructure.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Redis serializer that compresses values above a configurable threshold.
 * <p>
 * Compression is applied only to values >= threshold size. Values below
 * threshold
 * are stored uncompressed for fast access. Compression/decompression is
 * transparent
 * to the application.
 * <p>
 * Currently supports GZIP compression. LZ4 support can be added if needed.
 *
 * @param <T> value type
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class CacheCompressionSerializer<T> implements RedisSerializer<T> {

  private static final byte[] COMPRESSION_MARKER = new byte[] { 0x1F, (byte) 0x8B }; // GZIP magic bytes

  private final RedisSerializer<T> delegate;
  private final int compressionThreshold;
  private final CacheProperties.CompressionAlgorithm algorithm;

  public CacheCompressionSerializer(RedisSerializer<T> delegate, int compressionThreshold) {
    this(delegate, compressionThreshold, CacheProperties.CompressionAlgorithm.GZIP);
  }

  @Override
  public byte[] serialize(T value) throws SerializationException {
    if (value == null) {
      return delegate.serialize(null);
    }

    byte[] serialized = delegate.serialize(value);

    // Compress if above threshold
    if (serialized.length >= compressionThreshold) {
      try {
        byte[] compressed = compress(serialized);
        log.debug("Compressed cache value: {} bytes -> {} bytes (ratio: {:.2f}%)",
            serialized.length, compressed.length, (double) compressed.length / serialized.length * 100);
        return compressed;
      } catch (IOException e) {
        log.warn("Failed to compress cache value, storing uncompressed", e);
        return serialized;
      }
    }

    return serialized;
  }

  @Override
  public T deserialize(byte[] bytes) throws SerializationException {
    if (bytes == null || bytes.length == 0) {
      return delegate.deserialize(null);
    }

    // Check if compressed (starts with GZIP magic bytes)
    if (isCompressed(bytes)) {
      try {
        byte[] decompressed = decompress(bytes);
        log.debug("Decompressed cache value: {} bytes -> {} bytes",
            bytes.length, decompressed.length);
        return delegate.deserialize(decompressed);
      } catch (IOException e) {
        log.warn("Failed to decompress cache value", e);
        throw new SerializationException("Failed to decompress cache value", e);
      }
    }

    return delegate.deserialize(bytes);
  }

  /**
   * Check if bytes are compressed (starts with compression marker).
   */
  private boolean isCompressed(byte[] bytes) {
    if (bytes.length < COMPRESSION_MARKER.length) {
      return false;
    }
    for (int i = 0; i < COMPRESSION_MARKER.length; i++) {
      if (bytes[i] != COMPRESSION_MARKER[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compress bytes using configured algorithm.
   */
  private byte[] compress(byte[] data) throws IOException {
    switch (algorithm) {
      case GZIP:
        return compressGzip(data);
      case LZ4:
        // TODO: Implement LZ4 compression if library is available
        log.warn("LZ4 compression not yet implemented, falling back to GZIP");
        return compressGzip(data);
      default:
        return compressGzip(data);
    }
  }

  /**
   * Decompress bytes using configured algorithm.
   */
  private byte[] decompress(byte[] data) throws IOException {
    switch (algorithm) {
      case GZIP:
        return decompressGzip(data);
      case LZ4:
        // TODO: Implement LZ4 decompression if library is available
        log.warn("LZ4 decompression not yet implemented, falling back to GZIP");
        return decompressGzip(data);
      default:
        return decompressGzip(data);
    }
  }

  /**
   * Compress using GZIP.
   */
  private byte[] compressGzip(byte[] data) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
      gzos.write(data);
    }
    return baos.toByteArray();
  }

  /**
   * Decompress using GZIP.
   */
  private byte[] decompressGzip(byte[] data) throws IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = gzis.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
    }
    return baos.toByteArray();
  }
}
