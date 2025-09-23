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

// === ASYNC PATTERNS (V2 APIs) ===

// Operation Status for tracking async operations
enum TOperationStatus {
  PENDING = 1,
  IN_PROGRESS = 2,
  COMPLETED = 3,
  FAILED = 4,
  CANCELLED = 5
}

// Command Types
enum TCommandType {
  CREATE_USER = 1,
  UPDATE_USER = 2,
  DELETE_USER = 3
}

// Event Types
enum TEventType {
  USER_CREATED = 1,
  USER_UPDATED = 2,
  USER_DELETED = 3,
  USER_OPERATION_FAILED = 4
}

// Base Command structure for user.commands topic
struct TUserCommand {
  1: required string operationId,      // UUID for tracking
  2: required TCommandType commandType,
  3: required string correlationId,    // For tracing
  4: required i64 timestamp,
  5: optional string userId,           // For update/delete commands
  6: optional TUserCreateRequest createRequest,
  7: optional TUserUpdateRequest updateRequest,
  8: optional TUserDeleteRequest deleteRequest,
  9: optional string requestedBy       // User who initiated the operation
}

// Base Event structure for user.events topic
struct TUserEvent {
  1: required string eventId,          // UUID
  2: required TEventType eventType,
  3: required string operationId,      // Links back to command
  4: required string correlationId,
  5: required i64 timestamp,
  6: optional TUserResponse user,
  7: optional string errorMessage,     // For failed operations
  8: optional string errorCode
}

// Operation tracking
struct TOperationTracker {
  1: required string operationId,
  2: required TOperationStatus status,
  3: required i64 createdAt,
  4: optional i64 updatedAt,
  5: optional i64 completedAt,
  6: optional string result,           // JSON result for successful operations
  7: optional string errorMessage,
  8: optional string errorCode,
  9: required string correlationId
}

// Command Response (immediate response for async operations)
struct TCommandResponse {
  1: required string operationId,
  2: required TOperationStatus status,
  3: required string message,
  4: optional string trackingUrl       // URL to check operation status
}
