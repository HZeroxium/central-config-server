package com.example.user.adapter.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserMongoRepository extends MongoRepository<UserDocument, String> {
}
