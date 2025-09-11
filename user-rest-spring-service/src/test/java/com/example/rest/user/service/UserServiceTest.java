package com.example.rest.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.rest.user.domain.User;
import com.example.rest.user.port.ThriftUserClientPort;

class UserServiceTest {

  @Test
  void pingDelegates() {
    ThriftUserClientPort client = org.mockito.Mockito.mock(ThriftUserClientPort.class);
    when(client.ping()).thenReturn("pong");
    UserService svc = new UserService(client);
    assertEquals("pong", svc.ping());
  }

  @Test
  void crudDelegates() {
    ThriftUserClientPort client = org.mockito.Mockito.mock(ThriftUserClientPort.class);
    UserService svc = new UserService(client);
    User user = User.builder().id("1").name("A").phone("p").address("addr").build();

    when(client.create(any())).thenReturn(user);
    when(client.getById("1")).thenReturn(Optional.of(user));
    when(client.update(any())).thenReturn(user);
    when(client.list()).thenReturn(List.of(user));

    assertEquals("1", svc.create(user).getId());
    assertTrue(svc.getById("1").isPresent());
    assertEquals("1", svc.update(user).getId());
    assertFalse(svc.list().isEmpty());
    svc.delete("1");
    verify(client, times(1)).delete("1");
  }
}
