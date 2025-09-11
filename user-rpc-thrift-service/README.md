# User RPC Thrift Service (Java 21 + Spring Boot)

Build

- Prereqs: Java 21, Gradle, thrift compiler in PATH
- Commands:
  - gradlew clean build

Run locally

- Start Mongo and app via docker-compose:
  - docker compose up --build

Thrift IDL

- src/main/thrift/user_service.thrift

Generate Stubs

- Gradle will invoke thrift on compileJava; ensure `thrift` binary is installed.

Client Example (Java)

```java
TTransport transport = new TSocket("localhost", 9090);
transport.open();
TBinaryProtocol protocol = new TBinaryProtocol(transport);
UserService.Client client = new UserService.Client(protocol);
System.out.println(client.ping());
```

E2E Tests

- Uses Testcontainers MongoDB; run: `gradlew test`
