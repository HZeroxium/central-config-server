package com.example.rest.user.adapter.thrift;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.rest.user.domain.User;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.user.thrift.TUser;
import com.example.user.thrift.UserService;

@Component
@RequiredArgsConstructor
public class ThriftUserClientAdapter implements ThriftUserClientPort {

  @Value("${thrift.host}")
  private String host;

  @Value("${thrift.port}")
  private int port;

  private UserService.Client client() throws Exception {
    TTransport transport = new TSocket(host, port, 5000);
    transport.open();
    TBinaryProtocol protocol = new TBinaryProtocol(transport);
    return new UserService.Client(protocol);
  }

  private static TUser toThrift(User u) {
    return new TUser()
        .setId(u.getId())
        .setName(u.getName())
        .setPhone(u.getPhone())
        .setAddress(u.getAddress());
  }

  private static User toDomain(TUser t) {
    return User.builder()
        .id(t.getId())
        .name(t.getName())
        .phone(t.getPhone())
        .address(t.getAddress())
        .build();
  }

  @Override
  public String ping() {
    try {
      return client().ping();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public User create(User user) {
    try {
      return toDomain(client().createUser(toThrift(user)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<User> getById(String id) {
    try {
      TUser t = client().getUser(id);
      return Optional.ofNullable(t).map(ThriftUserClientAdapter::toDomain);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public User update(User user) {
    try {
      return toDomain(client().updateUser(toThrift(user)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(String id) {
    try {
      client().deleteUser(id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<User> list() {
    try {
      return client().listUsers().stream()
          .map(ThriftUserClientAdapter::toDomain)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
