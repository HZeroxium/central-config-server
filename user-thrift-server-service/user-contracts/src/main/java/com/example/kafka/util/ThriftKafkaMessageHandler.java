package com.example.kafka.util;

import com.example.kafka.serialization.ThriftMessageDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.thrift.TBase;

/**
 * Utility class for handling Thrift Kafka messages
 */
@Slf4j
public class ThriftKafkaMessageHandler {

  public static <T extends TBase<?, ?>> T deserializeMessage(ConsumerRecord<String, byte[]> record,
      Class<T> expectedType) {
    try {
      String topic = record.topic();
      byte[] data = record.value();

      if (data == null) {
        return null;
      }

      return ThriftMessageDeserializer.deserialize(topic, data, expectedType);
    } catch (Exception e) {
      log.error("Failed to deserialize Thrift message from topic {}: {}", record.topic(), e.getMessage(), e);
      throw new SerializationException("Failed to deserialize Thrift message", e);
    }
  }

  public static Object deserializeMessage(ConsumerRecord<String, byte[]> record) {
    try {
      String topic = record.topic();
      byte[] data = record.value();

      if (data == null) {
        return null;
      }

      return ThriftMessageDeserializer.deserializeByTopic(topic, data);
    } catch (Exception e) {
      log.error("Failed to deserialize Thrift message from topic {}: {}", record.topic(), e.getMessage(), e);
      throw new SerializationException("Failed to deserialize Thrift message", e);
    }
  }
}
