package com.example.demo.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "group_join_requests")
public class GroupJoinRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GroupJoinRequestStatus status = GroupJoinRequestStatus.PENDING;

    @Column(length = 255)
    private String message;

    public GroupJoinRequest() {
    }

    public GroupJoinRequest(Group group, User requester, String message) {
        this.group = group;
        this.requester = requester;
        this.message = message;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public GroupJoinRequestStatus getStatus() {
        return status;
    }

    public void setStatus(GroupJoinRequestStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void approve() {
        this.status = GroupJoinRequestStatus.APPROVED;
    }

    public void reject() {
        this.status = GroupJoinRequestStatus.REJECTED;
    }

    public void cancel() {
        this.status = GroupJoinRequestStatus.CANCELLED;
    }
}

