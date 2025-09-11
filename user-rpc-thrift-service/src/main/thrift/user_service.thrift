namespace java com.example.user.thrift

struct TUser {
  1: string id,
  2: string name,
  3: string phone,
  4: string address
}

struct TPagedUsers {
  1: list<TUser> items,
  2: i32 page,
  3: i32 size,
  4: i64 total,
  5: i32 totalPages
}

service UserService {
  string ping(),
  TUser createUser(1: TUser user),
  TUser getUser(1: string id),
  TUser updateUser(1: TUser user),
  void deleteUser(1: string id),
  TPagedUsers listUsers(1: i32 page, 2: i32 size)
}

