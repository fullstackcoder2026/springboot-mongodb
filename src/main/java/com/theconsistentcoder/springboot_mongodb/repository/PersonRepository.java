package com.theconsistentcoder.springboot_mongodb.repository;

import com.theconsistentcoder.springboot_mongodb.collection.Person;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface PersonRepository extends MongoRepository<Person, String> {

    List<Person> findByFirstNameStartsWith(String name);

    @Query(value = "{ 'age' : { $gt : ?0, $lt : ?1}}",
            fields = "{addresses:  0}")
    List<Person> findPersonByAgeBetween(Integer min, Integer max);

}
