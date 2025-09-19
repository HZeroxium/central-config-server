// user_service.thrift

namespace java com.example.user.thrift

// Enums
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

// Base User struct with audit fields and soft delete
struct TUser {
  1: string id,
  2: string name,
  3: string phone,
  4: string address,
  5: TUserStatus status = TUserStatus.ACTIVE,
  6: TUserRole role = TUserRole.USER,
  7: i64 createdAt,
  8: string createdBy = "admin",
  9: i64 updatedAt,
  10: string updatedBy = "admin",
  11: i32 version = 1,
  12: bool deleted = false,
  13: i64 deletedAt,
  14: string deletedBy
}

// Request/Response DTOs
struct TCreateUserRequest {
  1: string name,
  2: string phone,
  3: string address,
  4: TUserStatus status = TUserStatus.ACTIVE,
  5: TUserRole role = TUserRole.USER
}

struct TCreateUserResponse {
  1: i32 status,  // 0 = success, 1 = validation error, 2 = database error, etc.
  2: string message,
  3: TUser user
}

struct TGetUserRequest {
  1: string id
}

struct TGetUserResponse {
  1: i32 status,  // 0 = success, 1 = not found, 2 = database error, etc.
  2: string message,
  3: TUser user
}

struct TUpdateUserRequest {
  1: string id,
  2: string name,
  3: string phone,
  4: string address,
  5: TUserStatus status,
  6: TUserRole role,
  7: i32 version
}

struct TUpdateUserResponse {
  1: i32 status,  // 0 = success, 1 = not found, 2 = validation error, 3 = database error, etc.
  2: string message,
  3: TUser user
}

struct TDeleteUserRequest {
  1: string id
}

struct TDeleteUserResponse {
  1: i32 status,  // 0 = success, 1 = not found, 2 = database error, etc.
  2: string message
}

struct TListUsersRequest {
  // All fields are optional - validation handled by services
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

struct TListUsersResponse {
  1: i32 status,  // 0 = success, 1 = validation error, 2 = database error, etc.
  2: string message,
  3: list<TUser> items,
  4: i32 page,
  5: i32 size,
  6: i64 total,
  7: i32 totalPages
}

struct TPingResponse {
  1: i32 status,  // 0 = success, 1 = service error, etc.
  2: string message,
  3: string response
}

service UserService {
  // New structured API
  TPingResponse ping(),
  TCreateUserResponse createUser(1: TCreateUserRequest request),
  TGetUserResponse getUser(1: TGetUserRequest request),
  TUpdateUserResponse updateUser(1: TUpdateUserRequest request),
  TDeleteUserResponse deleteUser(1: TDeleteUserRequest request),
  TListUsersResponse listUsers(1: TListUsersRequest request)
}

