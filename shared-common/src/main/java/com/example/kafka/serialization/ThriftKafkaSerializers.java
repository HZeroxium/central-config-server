package com.example.kafka.serialization;

import com.example.kafka.thrift.*;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Factory class for creating Thrift-based Kafka serializers and deserializers
 * for specific message types
 */
public class ThriftKafkaSerializers {

  // Ping message serializers
  public static class TPingRequestSerializer extends ThriftSerializer<TPingRequest> {
  }

  public static class TPingRequestDeserializer extends ThriftDeserializer<TPingRequest> {
    public TPingRequestDeserializer() {
      super(TPingRequest.class);
    }
  }

  public static class TPingResponseSerializer extends ThriftSerializer<TPingResponse> {
  }

  public static class TPingResponseDeserializer extends ThriftDeserializer<TPingResponse> {
    public TPingResponseDeserializer() {
      super(TPingResponse.class);
    }
  }

  // User Create message serializers
  public static class TUserCreateRequestSerializer extends ThriftSerializer<TUserCreateRequest> {
  }

  public static class TUserCreateRequestDeserializer extends ThriftDeserializer<TUserCreateRequest> {
    public TUserCreateRequestDeserializer() {
      super(TUserCreateRequest.class);
    }
  }

  public static class TUserCreateResponseSerializer extends ThriftSerializer<TUserCreateResponse> {
  }

  public static class TUserCreateResponseDeserializer extends ThriftDeserializer<TUserCreateResponse> {
    public TUserCreateResponseDeserializer() {
      super(TUserCreateResponse.class);
    }
  }

  // User Get message serializers
  public static class TUserGetRequestSerializer extends ThriftSerializer<TUserGetRequest> {
  }

  public static class TUserGetRequestDeserializer extends ThriftDeserializer<TUserGetRequest> {
    public TUserGetRequestDeserializer() {
      super(TUserGetRequest.class);
    }
  }

  public static class TUserGetResponseSerializer extends ThriftSerializer<TUserGetResponse> {
  }

  public static class TUserGetResponseDeserializer extends ThriftDeserializer<TUserGetResponse> {
    public TUserGetResponseDeserializer() {
      super(TUserGetResponse.class);
    }
  }

  // User Update message serializers
  public static class TUserUpdateRequestSerializer extends ThriftSerializer<TUserUpdateRequest> {
  }

  public static class TUserUpdateRequestDeserializer extends ThriftDeserializer<TUserUpdateRequest> {
    public TUserUpdateRequestDeserializer() {
      super(TUserUpdateRequest.class);
    }
  }

  public static class TUserUpdateResponseSerializer extends ThriftSerializer<TUserUpdateResponse> {
  }

  public static class TUserUpdateResponseDeserializer extends ThriftDeserializer<TUserUpdateResponse> {
    public TUserUpdateResponseDeserializer() {
      super(TUserUpdateResponse.class);
    }
  }

  // User Delete message serializers
  public static class TUserDeleteRequestSerializer extends ThriftSerializer<TUserDeleteRequest> {
  }

  public static class TUserDeleteRequestDeserializer extends ThriftDeserializer<TUserDeleteRequest> {
    public TUserDeleteRequestDeserializer() {
      super(TUserDeleteRequest.class);
    }
  }

  public static class TUserDeleteResponseSerializer extends ThriftSerializer<TUserDeleteResponse> {
  }

  public static class TUserDeleteResponseDeserializer extends ThriftDeserializer<TUserDeleteResponse> {
    public TUserDeleteResponseDeserializer() {
      super(TUserDeleteResponse.class);
    }
  }

  // User List message serializers
  public static class TUserListRequestSerializer extends ThriftSerializer<TUserListRequest> {
  }

  public static class TUserListRequestDeserializer extends ThriftDeserializer<TUserListRequest> {
    public TUserListRequestDeserializer() {
      super(TUserListRequest.class);
    }
  }

  public static class TUserListResponseSerializer extends ThriftSerializer<TUserListResponse> {
  }

  public static class TUserListResponseDeserializer extends ThriftDeserializer<TUserListResponse> {
    public TUserListResponseDeserializer() {
      super(TUserListResponse.class);
    }
  }

  // V2 Async Command/Event serializers
  public static class TUserCommandSerializer extends ThriftSerializer<TUserCommand> {
  }

  public static class TUserCommandDeserializer extends ThriftDeserializer<TUserCommand> {
    public TUserCommandDeserializer() {
      super(TUserCommand.class);
    }
  }

  public static class TUserEventSerializer extends ThriftSerializer<TUserEvent> {
  }

  public static class TUserEventDeserializer extends ThriftDeserializer<TUserEvent> {
    public TUserEventDeserializer() {
      super(TUserEvent.class);
    }
  }

  public static class TOperationTrackerSerializer extends ThriftSerializer<TOperationTracker> {
  }

  public static class TOperationTrackerDeserializer extends ThriftDeserializer<TOperationTracker> {
    public TOperationTrackerDeserializer() {
      super(TOperationTracker.class);
    }
  }

  public static class TCommandResponseSerializer extends ThriftSerializer<TCommandResponse> {
  }

  public static class TCommandResponseDeserializer extends ThriftDeserializer<TCommandResponse> {
    public TCommandResponseDeserializer() {
      super(TCommandResponse.class);
    }
  }

  // Generic Object serializers for when we need to handle multiple types
  public static class ThriftObjectSerializer implements Serializer<Object> {

    @Override
    public void configure(java.util.Map<String, ?> configs, boolean isKey) {
      // No configuration needed
    }

    @Override
    public byte[] serialize(String topic, Object data) {
      if (data instanceof com.example.kafka.thrift.TPingRequest) {
        return new TPingRequestSerializer().serialize(topic, (TPingRequest) data);
      } else if (data instanceof com.example.kafka.thrift.TPingResponse) {
        return new TPingResponseSerializer().serialize(topic, (TPingResponse) data);
      } else if (data instanceof com.example.kafka.thrift.TUserCreateRequest) {
        return new TUserCreateRequestSerializer().serialize(topic, (TUserCreateRequest) data);
      } else if (data instanceof com.example.kafka.thrift.TUserCreateResponse) {
        return new TUserCreateResponseSerializer().serialize(topic, (TUserCreateResponse) data);
      } else if (data instanceof com.example.kafka.thrift.TUserGetRequest) {
        return new TUserGetRequestSerializer().serialize(topic, (TUserGetRequest) data);
      } else if (data instanceof com.example.kafka.thrift.TUserGetResponse) {
        return new TUserGetResponseSerializer().serialize(topic, (TUserGetResponse) data);
      } else if (data instanceof com.example.kafka.thrift.TUserUpdateRequest) {
        return new TUserUpdateRequestSerializer().serialize(topic, (TUserUpdateRequest) data);
      } else if (data instanceof com.example.kafka.thrift.TUserUpdateResponse) {
        return new TUserUpdateResponseSerializer().serialize(topic, (TUserUpdateResponse) data);
      } else if (data instanceof com.example.kafka.thrift.TUserDeleteRequest) {
        return new TUserDeleteRequestSerializer().serialize(topic, (TUserDeleteRequest) data);
      } else if (data instanceof com.example.kafka.thrift.TUserDeleteResponse) {
        return new TUserDeleteResponseSerializer().serialize(topic, (TUserDeleteResponse) data);
      } else if (data instanceof com.example.kafka.thrift.TUserListRequest) {
        return new TUserListRequestSerializer().serialize(topic, (TUserListRequest) data);
      } else if (data instanceof com.example.kafka.thrift.TUserListResponse) {
        return new TUserListResponseSerializer().serialize(topic, (TUserListResponse) data);
      } else if (data instanceof com.example.kafka.thrift.TUserCommand) {
        return new TUserCommandSerializer().serialize(topic, (TUserCommand) data);
      } else if (data instanceof com.example.kafka.thrift.TUserEvent) {
        return new TUserEventSerializer().serialize(topic, (TUserEvent) data);
      } else if (data instanceof com.example.kafka.thrift.TOperationTracker) {
        return new TOperationTrackerSerializer().serialize(topic, (TOperationTracker) data);
      } else if (data instanceof com.example.kafka.thrift.TCommandResponse) {
        return new TCommandResponseSerializer().serialize(topic, (TCommandResponse) data);
      } else {
        throw new IllegalArgumentException("Unsupported Thrift type: " + data.getClass());
      }
    }

    @Override
    public void close() {
      // No resources to close
    }
  }
}
