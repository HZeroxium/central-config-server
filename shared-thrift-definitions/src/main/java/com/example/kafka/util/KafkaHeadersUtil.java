package com.example.kafka.util;

import com.example.kafka.constants.KafkaConstants;
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
        record.headers().add(KafkaConstants.HEADER_SAGA_ID, sagaId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaConstants.HEADER_CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaConstants.HEADER_CAUSATION_ID, causationId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaConstants.HEADER_EVENT_ID, eventId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaConstants.HEADER_SOURCE, source.getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaConstants.HEADER_TYPE, type.getBytes(StandardCharsets.UTF_8));
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
        return getHeaderAsString(headers, KafkaConstants.HEADER_SAGA_ID);
    }
    
    public static String getCorrelationId(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, KafkaConstants.HEADER_CORRELATION_ID);
    }
    
    public static String getCausationId(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, KafkaConstants.HEADER_CAUSATION_ID);
    }
    
    public static String getEventId(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, KafkaConstants.HEADER_EVENT_ID);
    }
    
    public static String getSource(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, KafkaConstants.HEADER_SOURCE);
    }
    
    public static String getType(org.apache.kafka.common.header.Headers headers) {
        return getHeaderAsString(headers, KafkaConstants.HEADER_TYPE);
    }
}
