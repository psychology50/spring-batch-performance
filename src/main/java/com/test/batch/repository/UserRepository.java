package com.test.batch.repository;

import com.test.batch.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select u.id from User u")
    List<Long> findAllIds();
}
