package com.example.demo.Repositories;

import com.example.demo.Models.Group;
import com.example.demo.Models.GroupJoinRequest;
import com.example.demo.Models.GroupJoinRequestStatus;
import com.example.demo.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    Optional<GroupJoinRequest> findByIdAndDeletedFalse(Long id);

    List<GroupJoinRequest> findAllByGroupAndDeletedFalse(Group group);

    Optional<GroupJoinRequest> findByGroupAndRequesterAndStatusAndDeletedFalse(Group group, User requester, GroupJoinRequestStatus status);

    List<GroupJoinRequest> findAllByGroupAndStatusAndDeletedFalse(Group group, GroupJoinRequestStatus status);
}

