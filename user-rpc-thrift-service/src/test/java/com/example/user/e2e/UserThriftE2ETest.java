package com.example.user.e2e;

import com.example.user.Application;
import com.example.user.thrift.TUser;
import com.example.user.thrift.UserService;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserThriftE2ETest {

  @Container
  private static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

  private ConfigurableApplicationContext context;

  @BeforeAll
  void startApp() {
    mongo.start();
    String mongoUri = mongo.getReplicaSetUrl("users");
    context = SpringApplication.run(Application.class,
        "--spring.data.mongodb.uri=" + mongoUri,
        "--thrift.port=19090");
  }

  @AfterAll
  void stopApp() {
    if (context != null) {
      context.close();
    }
    mongo.stop();
  }

  private UserService.Client thriftClient(int port) throws Exception {
    TTransport transport = new TSocket("127.0.0.1", port, 5000);
    transport.open();
    TBinaryProtocol protocol = new TBinaryProtocol(transport);
    return new UserService.Client(protocol);
  }

  @Test
  void pingAndCrud() throws Exception {
    UserService.Client client = thriftClient(19090);

    String pong = client.ping();
    assertEquals("pong", pong);

    TUser created = client.createUser(new TUser()
        .setId("u1")
        .setName("Alice")
        .setPhone("111-222")
        .setAddress("Earth"));
    assertEquals("u1", created.getId());

    TUser fetched = client.getUser("u1");
    assertNotNull(fetched);
    assertEquals("Alice", fetched.getName());

    fetched.setName("Alice Updated");
    TUser updated = client.updateUser(fetched);
    assertEquals("Alice Updated", updated.getName());

    assertFalse(client.listUsers().isEmpty());

    client.deleteUser("u1");
    assertNull(client.getUser("u1"));
  }
}
