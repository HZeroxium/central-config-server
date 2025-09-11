namespace java com.example.user.thrift

struct TUser {
  1: string id,
  2: string name,
  3: string phone,
  4: string address
}

service UserService {
  string ping(),
  TUser createUser(1: TUser user),
  TUser getUser(1: string id),
  TUser updateUser(1: TUser user),
  void deleteUser(1: string id),
  list<TUser> listUsers()
}

