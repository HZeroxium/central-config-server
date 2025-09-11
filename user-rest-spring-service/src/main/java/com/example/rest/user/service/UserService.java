package com.example.rest.user.service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.rest.user.domain.User;
import com.example.rest.user.port.ThriftUserClientPort;

@Service
@RequiredArgsConstructor
public class UserService {
  private final ThriftUserClientPort thriftClient;

  public String ping() {
    return thriftClient.ping();
  }

  public User create(User user) {
    return thriftClient.create(user);
  }

  public Optional<User> getById(String id) {
    return thriftClient.getById(id);
  }

  public User update(User user) {
    return thriftClient.update(user);
  }

  public void delete(String id) {
    thriftClient.delete(id);
  }

  public List<User> list() {
    return thriftClient.list();
  }
}
