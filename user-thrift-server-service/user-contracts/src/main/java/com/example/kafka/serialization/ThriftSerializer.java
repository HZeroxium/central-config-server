package com.example.kafka.serialization;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Custom Kafka serializer for Thrift objects
 * Uses TBinaryProtocol for efficient serialization
 */
@Slf4j
public class ThriftSerializer<T extends TBase<?, ?>> implements Serializer<T> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // No specific configuration needed
  }

  @Override
  public byte[] serialize(String topic, T data) {
    if (data == null) {
      return null;
    }

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      TIOStreamTransport transport = new TIOStreamTransport(baos);
      TBinaryProtocol protocol = new TBinaryProtocol(transport);
      data.write(protocol);
      byte[] result = baos.toByteArray();
      log.debug("Serialized Thrift object for topic {}: {} bytes", topic, result.length);
      return result;
    } catch (Exception e) {
      log.error("Error serializing Thrift object for topic {}: {}", topic, e.getMessage(), e);
      throw new SerializationException("Error serializing Thrift object for topic " + topic, e);
    }
  }

  @Override
  public void close() {
    // No resources to close
  }
}
