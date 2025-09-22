// kafka_messages.thrift - Kafka message schemas using Thrift

namespace java com.example.kafka.thrift

// Redefine enums locally to avoid include issues
enum TUserStatus {
  ACTIVE = 1,
  INACTIVE = 2,
  SUSPENDED = 3
}

enum TUserRole {
  ADMIN = 1,
  USER = 2,
  MODERATOR = 3,
  GUEST = 4
}

// Sort criterion for flexible sorting
struct SortCriterion {
  1: required string fieldName,
  2: required string direction  // "asc" or "desc"
}

// Ping messages
struct TPingRequest {
  1: required string message = "ping"
}

struct TPingResponse {
  1: required string message
}

// User Create messages
struct TUserCreateRequest {
  1: required string name,
  2: required string phone, 
  3: required string address,
  4: required TUserStatus status = TUserStatus.ACTIVE,
  5: required TUserRole role = TUserRole.USER
}

struct TUserCreateResponse {
  1: required TUserResponse user
}

// User Get messages  
struct TUserGetRequest {
  1: required string id
}

struct TUserGetResponse {
  1: optional TUserResponse user,
  2: required bool found
}

// User Update messages
struct TUserUpdateRequest {
  1: required string id,
  2: required string name,
  3: required string phone,
  4: required string address, 
  5: required TUserStatus status,
  6: required TUserRole role,
  7: required i32 version
}

struct TUserUpdateResponse {
  1: optional TUserResponse user,
  2: required bool success
}

// User Delete messages
struct TUserDeleteRequest {
  1: required string id
}

struct TUserDeleteResponse {
  1: required bool deleted
}

// User List messages
struct TUserListRequest {
  1: optional i32 page,
  2: optional i32 size,
  3: optional string search,
  4: optional TUserStatus status,
  5: optional TUserRole role,
  6: optional bool includeDeleted,
  7: optional i64 createdAfter,
  8: optional i64 createdBefore,
  9: optional list<SortCriterion> sortCriteria
}

struct TUserListResponse {
  1: required list<TUserResponse> items,
  2: required i32 page,
  3: required i32 size,
  4: required i64 total,
  5: required i32 totalPages
}

// Common User Response structure for Kafka
struct TUserResponse {
  1: required string id,
  2: required string name,
  3: required string phone,
  4: required string address,
  5: required TUserStatus status,
  6: required TUserRole role,
  7: optional i64 createdAt,
  8: optional string createdBy,
  9: optional i64 updatedAt,
  10: optional string updatedBy,
  11: optional i32 version,
  12: optional bool deleted,
  13: optional i64 deletedAt,
  14: optional string deletedBy
}
