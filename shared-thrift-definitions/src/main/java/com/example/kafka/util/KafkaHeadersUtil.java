package com.example.kafka.util;

import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utility class for managing Kafka headers consistently across services
 */
public class KafkaHeadersUtil {
    
    public static void addStandardHeaders(ProducerRecord<String, String> record, 
                                        String sagaId, 
                                        String correlationId, 
                                        String causationId, 
                                        String eventId, 
                                        String source, 
                                        String type) {
        record.headers().add("sagaId", sagaId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("causationId", causationId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventId", eventId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("source", source.getBytes(StandardCharsets.UTF_8));
        record.headers().add("type", type.getBytes(StandardCharsets.UTF_8));
    }
    
    public static void addStandardHeaders(ProducerRecord<String, String> record, 
                                        String sagaId, 
                                        String source, 
                                        String type) {
        addStandardHeaders(record, sagaId, sagaId, UUID.randomUUID().toString(), 
                          UUID.randomUUID().toString(), source, type);
    }
    
    public static String getHeaderAsString(org.apache.kafka.common.header.Headers headers, String key) {
        var header = headers.lastHeader(key);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }
    
    public static String getSagaId(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, "sagaId");
    }
    
    public static String getCorrelationId(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, "correlationId");
    }
    
    public static String getCausationId(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, "causationId");
    }
    
    public static String getEventId(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, "eventId");
    }
    
    public static String getSource(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, "source");
    }
    
    public static String getType(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, "type");
    }
}
