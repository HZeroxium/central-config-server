package com.example.kafka.serialization;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Avro deserializer for Kafka messages using Confluent Schema Registry.
 * Provides type-safe deserialization for SpecificRecord objects.
 */
public class AvroDeserializer<T> implements Deserializer<T> {

  private final KafkaAvroDeserializer kafkaAvroDeserializer = new KafkaAvroDeserializer();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    kafkaAvroDeserializer.configure(configs, isKey);
  }

  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(String topic, byte[] data) {
    if (data == null) {
      return null;
    }
    return (T) kafkaAvroDeserializer.deserialize(topic, data);
  }

  @Override
  public void close() {
    kafkaAvroDeserializer.close();
  }
}
