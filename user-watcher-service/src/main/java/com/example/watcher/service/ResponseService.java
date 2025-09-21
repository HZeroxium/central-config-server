package com.example.watcher.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseService {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void sendResponse(String replyTopic, String correlationId, Object response) {
    try {
      ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(replyTopic, response);
      producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes());

      kafkaTemplate.send(producerRecord);
      log.debug("Successfully sent response to topic: {} with correlationId: {}", replyTopic, correlationId);
    } catch (Exception e) {
      log.error("Error sending response to topic: {} with correlationId: {}", replyTopic, correlationId, e);
      throw new RuntimeException("Failed to send response", e);
    }
  }
}
