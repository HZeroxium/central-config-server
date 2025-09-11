package com.example.user.adapter.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document for collection {@code users}.
 * Serves as persistence representation for the domain user when Mongo is active.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class UserDocument {
  @Id
  private String id;
  private String name;
  private String phone;
  private String address;
}
