namespace java com.example.user.thrift

// Base User struct
struct TUser {
  1: string id,
  2: string name,
  3: string phone,
  4: string address
}

// Request/Response DTOs
struct TCreateUserRequest {
  1: string name,
  2: string phone,
  3: string address
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
  4: string address
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
  1: i32 page,
  2: i32 size
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

// Legacy structs for backward compatibility
struct TPagedUsers {
  1: list<TUser> items,
  2: i32 page,
  3: i32 size,
  4: i64 total,
  5: i32 totalPages
}

service UserService {
  // New structured API
  TPingResponse ping(),
  TCreateUserResponse createUser(1: TCreateUserRequest request),
  TGetUserResponse getUser(1: TGetUserRequest request),
  TUpdateUserResponse updateUser(1: TUpdateUserRequest request),
  TDeleteUserResponse deleteUser(1: TDeleteUserRequest request),
  TListUsersResponse listUsers(1: TListUsersRequest request),
  
  // Legacy API for backward compatibility
  string pingLegacy(),
  TUser createUserLegacy(1: TUser user),
  TUser getUserLegacy(1: string id),
  TUser updateUserLegacy(1: TUser user),
  void deleteUserLegacy(1: string id),
  TPagedUsers listUsersLegacy(1: i32 page, 2: i32 size)
}

