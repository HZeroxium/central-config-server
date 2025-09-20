package com.example.kafka.serialization;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Avro serializer for Kafka messages using Confluent Schema Registry.
 * Provides type-safe serialization for SpecificRecord objects.
 */
public class AvroSerializer<T> implements Serializer<T> {

  private final KafkaAvroSerializer kafkaAvroSerializer = new KafkaAvroSerializer();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    kafkaAvroSerializer.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(String topic, T data) {
    if (data == null) {
      return null;
    }
    return kafkaAvroSerializer.serialize(topic, data);
  }

  @Override
  public void close() {
    kafkaAvroSerializer.close();
  }
}
