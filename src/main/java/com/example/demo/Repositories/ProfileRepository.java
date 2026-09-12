package com.example.demo.Repositories;

import com.example.demo.Models.Profile;
import com.example.demo.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    List<Profile> findAllByDeletedFalse();

    Optional<Profile> findByIdAndDeletedFalse(Long id);

    Optional<Profile> findByOwnerAndDeletedFalse(User owner);
}

