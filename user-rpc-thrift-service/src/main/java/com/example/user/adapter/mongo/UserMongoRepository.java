package com.example.user.adapter.mongo;

import com.example.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Spring Data Mongo repository for {@link UserDocument}. */
public interface UserMongoRepository extends MongoRepository<UserDocument, String> {
  
  @Query("{ '_id': ?0, $or: [{ 'deleted': { $exists: false } }, { 'deleted': false }] }")
  Optional<UserDocument> findByIdAndNotDeleted(String id);
  
  @Query("{ $or: [{ 'deleted': { $exists: false } }, { 'deleted': false }] }")
  List<UserDocument> findAllNotDeleted();
  
  @Query("{ $or: [{ 'deleted': { $exists: false } }, { 'deleted': false }] }")
  Page<UserDocument> findAllNotDeleted(Pageable pageable);
  
  @Query(value = "{ $or: [{ 'deleted': { $exists: false } }, { 'deleted': false }] }", count = true)
  long countNotDeleted();
  
  // Advanced query methods
  @Query("{ $and: [ " +
         "{ $or: [{ 'deleted': { $exists: false } }, { 'deleted': ?0 }] }, " +
         "{ $or: [ " +
           "{ 'name': { $regex: ?1, $options: 'i' } }, " +
           "{ 'phone': { $regex: ?1, $options: 'i' } }, " +
           "{ 'address': { $regex: ?1, $options: 'i' } } " +
         "] }, " +
         "{ $expr: { $cond: { if: { $ne: [?2, null] }, then: { $eq: ['$status', ?2] }, else: true } } }, " +
         "{ $expr: { $cond: { if: { $ne: [?3, null] }, then: { $eq: ['$role', ?3] }, else: true } } }, " +
         "{ $expr: { $cond: { if: { $ne: [?4, null] }, then: { $gte: ['$createdAt', ?4] }, else: true } } }, " +
         "{ $expr: { $cond: { if: { $ne: [?5, null] }, then: { $lte: ['$createdAt', ?5] }, else: true } } } " +
         "] }")
  List<UserDocument> findByAdvancedCriteria(
      Boolean includeDeleted,
      String search,
      User.UserStatus status,
      User.UserRole role,
      LocalDateTime createdAfter,
      LocalDateTime createdBefore,
      Pageable pageable);
  
  @Query(value = "{ $and: [ " +
         "{ $or: [{ 'deleted': { $exists: false } }, { 'deleted': ?0 }] }, " +
         "{ $or: [ " +
           "{ 'name': { $regex: ?1, $options: 'i' } }, " +
           "{ 'phone': { $regex: ?1, $options: 'i' } }, " +
           "{ 'address': { $regex: ?1, $options: 'i' } } " +
         "] }, " +
         "{ $expr: { $cond: { if: { $ne: [?2, null] }, then: { $eq: ['$status', ?2] }, else: true } } }, " +
         "{ $expr: { $cond: { if: { $ne: [?3, null] }, then: { $eq: ['$role', ?3] }, else: true } } }, " +
         "{ $expr: { $cond: { if: { $ne: [?4, null] }, then: { $gte: ['$createdAt', ?4] }, else: true } } }, " +
         "{ $expr: { $cond: { if: { $ne: [?5, null] }, then: { $lte: ['$createdAt', ?5] }, else: true } } } " +
         "] }", count = true)
  long countByAdvancedCriteria(
      Boolean includeDeleted,
      String search,
      User.UserStatus status,
      User.UserRole role,
      LocalDateTime createdAfter,
      LocalDateTime createdBefore);
}
