package com.example.watcher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Validated
@ConfigurationProperties(prefix = "app.persistence")
public class PersistenceProperties {

  @Pattern(regexp = "mongo|jpa")
  private String type = "mongo";

  @NotBlank
  private String mongodbUri = "mongodb://localhost:27017/users";

  @NotBlank
  private String jdbcUrl = "jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1;MODE=LEGACY";

  @NotBlank
  private String jdbcDriver = "org.h2.Driver";

  @NotBlank
  private String jdbcUsername = "sa";

  @NotBlank
  private String jdbcPassword = "password";
}
