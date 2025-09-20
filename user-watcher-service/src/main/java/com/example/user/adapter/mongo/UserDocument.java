package com.example.user.adapter.mongo;

import com.example.common.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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
  private User.UserStatus status;
  private User.UserRole role;
  private LocalDateTime createdAt;
  private String createdBy;
  private LocalDateTime updatedAt;
  private String updatedBy;
  @Version
  private Integer version;
  private Boolean deleted;
  private LocalDateTime deletedAt;
  private String deletedBy;
}
