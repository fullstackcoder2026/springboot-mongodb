package com.theconsistentcoder.springboot_mongodb.service;

import com.theconsistentcoder.springboot_mongodb.collection.Person;
import com.theconsistentcoder.springboot_mongodb.repository.PersonRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class PersonServiceImpl implements PersonService{

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String save(Person person) {
        return personRepository.save(person).getPersonId();
    }

    @Override
    public List<Person> getPersonStartWith(String name) {
        return personRepository.findByFirstNameStartsWith(name);
    }

    @Override
    public void delete(String id) {
        personRepository.deleteById(id);

    }

    @Override
    public List<Person> getByPersonAge(Integer minAge, Integer maxAge) {
        return personRepository.findPersonByAgeBetween(minAge, maxAge);
    }

    @Override
    public Page<Person> search(String name, Integer minAge, Integer maxAge,
                                     String city, Pageable pageable) {
        Query query = new Query().with(pageable);
        List<Criteria> criteria = new ArrayList<>();

        // Add filters only if provided
        if (StringUtils.hasText(name)) {
            criteria.add(Criteria.where("firstName").regex(name, "i"));
        }

        if (minAge != null && maxAge != null) {
            criteria.add(Criteria.where("age").gte(minAge).lte(maxAge));
        }

        if (StringUtils.hasText(city)) {
            criteria.add(Criteria.where("addresses.city").is(city));
        }

        // Combine all criteria with AND
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(
                    criteria.toArray(new Criteria[0])
            ));
        }

        // Execute query and get paginated results
        List<Person> people = mongoTemplate.find(query, Person.class);
        long count = mongoTemplate.count(
                Query.of(query).limit(-1).skip(-1), Person.class
        );

        return PageableExecutionUtils.getPage(people, pageable, () -> count);
    }

    @Override
    public List<Document> getOldestPersonByCity() {
        Aggregation aggregation = Aggregation.newAggregation(
       // step1 : Unwind addresses array to create seaparte documents
        Aggregation.unwind("addresses"),
        //step2 : sort by age (oldest first)
        Aggregation.sort(Sort.Direction.DESC, "age"),

        // step 3: group by city and take first oldest person
        Aggregation.group("addresses.city")
                .first(Aggregation.ROOT)
                .as("oldestPerson")
                );
        return mongoTemplate.aggregate(aggregation, Person.class, Document.class)
                .getMappedResults();
    }

    @Override
    public List<Document> getPopulationByCity() {
        Aggregation aggregation = Aggregation.newAggregation(
                // step1 : Unwind addresses array to create seaparte documents
                Aggregation.unwind("addresses"),
                // Step 2: Group by city and count
                Aggregation.group("addresses.city").count().as("popCount"),
                //step3 : sort by population desc
                Aggregation.sort(Sort.Direction.DESC, "popCount"),

                // step 4: cleanup fields
                Aggregation.project()
                        .andExpression("_id").as("city")
                        .andExpression("popCount").as("count")
                        .andExclude("_id")
        );
        return mongoTemplate
                .aggregate(aggregation, Person.class, Document.class)
                .getMappedResults();
    }
}
