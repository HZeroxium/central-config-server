package com.example.rest.user.port;

import java.util.List;
import java.util.Optional;

import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;

public interface ThriftUserClientPort {
  String ping();

  User create(User user);

  Optional<User> getById(String id);

  User update(User user);

  void delete(String id);

  List<User> listByCriteria(UserQueryCriteria criteria);

  long countByCriteria(UserQueryCriteria criteria);
}
