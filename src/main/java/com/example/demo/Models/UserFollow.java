package com.example.demo.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "user_follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "target_id"}))
public class UserFollow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    public User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    public User target;
}
