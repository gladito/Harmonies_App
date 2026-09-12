package com.example.demo.Repositories;

import com.example.demo.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByDeletedFalse();

    Optional<User> findByIdAndDeletedFalse(Long id);

    Optional<User> findByUsernameAndDeletedFalse(String username);

	Optional<User> findByEmailAndDeletedFalse(String email);

	boolean existsByEmailAndDeletedFalse(String email);

	boolean existsByUsernameAndDeletedFalse(String username);
}

