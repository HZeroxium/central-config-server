package com.example.user.adapter.jpa;

import com.example.common.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 * Only used when app.persistence.type=h2 (or any JPA-backed profile).
 */
// Dung extension de generate ra query SQL dua tren ten ham
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
  
  @Query("SELECT u FROM UserEntity u WHERE u.id = :id AND (u.deleted = false OR u.deleted IS NULL)")
  Optional<UserEntity> findByIdAndNotDeleted(@Param("id") String id);
  
  @Query("SELECT u FROM UserEntity u WHERE u.deleted = false OR u.deleted IS NULL")
  List<UserEntity> findAllNotDeleted();
  
  @Query("SELECT u FROM UserEntity u WHERE u.deleted = false OR u.deleted IS NULL")
  Page<UserEntity> findAllNotDeleted(Pageable pageable);
  
  @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.deleted = false OR u.deleted IS NULL")
  long countNotDeleted();
  
  // Advanced query methods
  @Query("SELECT u FROM UserEntity u WHERE " +
         "(:includeDeleted = true OR u.deleted = false OR u.deleted IS NULL) AND " +
         "(:search IS NULL OR :search = '' OR " +
         " LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
         " LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
         " LOWER(u.address) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
         "(:status IS NULL OR u.status = :status) AND " +
         "(:role IS NULL OR u.role = :role) AND " +
         "(:createdAfter IS NULL OR u.createdAt >= :createdAfter) AND " +
         "(:createdBefore IS NULL OR u.createdAt <= :createdBefore)")
  List<UserEntity> findByAdvancedCriteria(
      @Param("includeDeleted") Boolean includeDeleted,
      @Param("search") String search,
      @Param("status") User.UserStatus status,
      @Param("role") User.UserRole role,
      @Param("createdAfter") LocalDateTime createdAfter,
      @Param("createdBefore") LocalDateTime createdBefore,
      Pageable pageable);
  
  @Query("SELECT COUNT(u) FROM UserEntity u WHERE " +
         "(:includeDeleted = true OR u.deleted = false OR u.deleted IS NULL) AND " +
         "(:search IS NULL OR :search = '' OR " +
         " LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
         " LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
         " LOWER(u.address) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
         "(:status IS NULL OR u.status = :status) AND " +
         "(:role IS NULL OR u.role = :role) AND " +
         "(:createdAfter IS NULL OR u.createdAt >= :createdAfter) AND " +
         "(:createdBefore IS NULL OR u.createdAt <= :createdBefore)")
  long countByAdvancedCriteria(
      @Param("includeDeleted") Boolean includeDeleted,
      @Param("search") String search,
      @Param("status") User.UserStatus status,
      @Param("role") User.UserRole role,
      @Param("createdAfter") LocalDateTime createdAfter,
      @Param("createdBefore") LocalDateTime createdBefore);


}


