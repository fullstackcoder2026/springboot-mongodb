package com.theconsistentcoder.springboot_mongodb.repository;

import com.theconsistentcoder.springboot_mongodb.collection.Photo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PhotoRepository extends MongoRepository<Photo, String> {
}
