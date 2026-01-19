package com.theconsistentcoder.springboot_mongodb.collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@Document(collection = "person")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Person {

    private String personId;
    private String firstName;
    private String lastName;
    private Integer age;
    private List<String> hobbies;
    private List<Address> addresses;
}
