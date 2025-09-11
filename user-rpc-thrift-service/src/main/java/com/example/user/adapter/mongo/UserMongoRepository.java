package com.example.user.adapter.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Spring Data Mongo repository for {@link UserDocument}. */
public interface UserMongoRepository extends MongoRepository<UserDocument, String> {}
