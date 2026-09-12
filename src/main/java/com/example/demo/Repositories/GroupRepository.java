package com.example.demo.Repositories;

import com.example.demo.Models.Group;
import com.example.demo.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findAllByDeletedFalse();

    Optional<Group> findByIdAndDeletedFalse(Long id);

    Optional<Group> findByNameAndDeletedFalse(String name);

    boolean existsByNameAndDeletedFalse(String name);

    List<Group> findAllByMembersContainsAndDeletedFalse(User user);

    List<Group> findAllByOwnerAndDeletedFalse(User owner);
}

