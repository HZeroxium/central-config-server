package com.example.kafka.serialization;

import com.example.kafka.thrift.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for deserializing Thrift messages based on topic names
 */
@Slf4j
public class ThriftMessageDeserializer {

  private static final Map<String, Class<? extends TBase<?, ?>>> TOPIC_TYPE_MAP;

  static {
    Map<String, Class<? extends TBase<?, ?>>> map = new HashMap<>();
    map.put("ping.request", TPingRequest.class);
    map.put("ping.response", TPingResponse.class);
    map.put("user.create.request", TUserCreateRequest.class);
    map.put("user.create.response", TUserCreateResponse.class);
    map.put("user.get.request", TUserGetRequest.class);
    map.put("user.get.response", TUserGetResponse.class);
    map.put("user.update.request", TUserUpdateRequest.class);
    map.put("user.update.response", TUserUpdateResponse.class);
    map.put("user.delete.request", TUserDeleteRequest.class);
    map.put("user.delete.response", TUserDeleteResponse.class);
    map.put("user.list.request", TUserListRequest.class);
    map.put("user.list.response", TUserListResponse.class);
    // V2 Async patterns
    map.put("user.commands", TUserCommand.class);
    map.put("user.events", TUserEvent.class);
    map.put("user.operations", TOperationTracker.class);
    TOPIC_TYPE_MAP = Map.copyOf(map);
  }

  public static <T extends TBase<?, ?>> T deserialize(String topic, byte[] data, Class<T> expectedType) {
    if (data == null) {
      return null;
    }

    try {
      Constructor<T> constructor = expectedType.getConstructor();
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

  public static Object deserializeByTopic(String topic, byte[] data) {
    Class<? extends TBase<?, ?>> typeClass = TOPIC_TYPE_MAP.get(topic);
    if (typeClass == null) {
      throw new SerializationException("Unknown topic for Thrift deserialization: " + topic);
    }

    return deserialize(topic, data, typeClass);
  }

  public static <T extends TBase<?, ?>> Class<T> getTypeForTopic(String topic) {
    return (Class<T>) TOPIC_TYPE_MAP.get(topic);
  }
}
