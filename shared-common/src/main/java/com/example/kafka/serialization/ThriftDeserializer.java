package com.example.kafka.serialization;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Custom Kafka deserializer for Thrift objects
 * Uses TBinaryProtocol for efficient deserialization
 */
@Slf4j
public class ThriftDeserializer<T extends TBase<?, ?>> implements Deserializer<T> {

  private Class<T> thriftClass;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    String className = (String) configs.get("thrift.class");
    if (className != null) {
      try {
        this.thriftClass = (Class<T>) Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new SerializationException("Thrift class not found: " + className, e);
      }
    }
  }

  public ThriftDeserializer(Class<T> thriftClass) {
    this.thriftClass = thriftClass;
  }

  public ThriftDeserializer() {
    // Default constructor for Kafka configuration
  }

  @Override
  public T deserialize(String topic, byte[] data) {
    if (data == null) {
      return null;
    }

    if (thriftClass == null) {
      throw new SerializationException("Thrift class not configured for deserializer");
    }

    try {
      Constructor<T> constructor = thriftClass.getConstructor();
      T instance = constructor.newInstance();

      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      TIOStreamTransport transport = new TIOStreamTransport(bais);
      TBinaryProtocol protocol = new TBinaryProtocol(transport);

      instance.read(protocol);
      log.debug("Deserialized Thrift object for topic {}: {} bytes", topic, data.length);
      return instance;
    } catch (Exception e) {
      log.error("Error deserializing Thrift object for topic {}: {}", topic, e.getMessage(), e);
      throw new SerializationException("Error deserializing Thrift object for topic " + topic, e);
    }
  }

  @Override
  public void close() {
    // No resources to close
  }
}
