package com.example.demo.Models;
import jakarta.persistence.*;
@Entity @Table(name="post_reactions",uniqueConstraints=@UniqueConstraint(columnNames={"post_id","user_id"}))
public class PostReaction extends BaseEntity {
@ManyToOne(optional=false) @JoinColumn(name="post_id") public Post post;
@ManyToOne(optional=false) @JoinColumn(name="user_id") public User user;
@Column(name="reaction_value",nullable=false) @org.hibernate.annotations.Check(constraints="reaction_value in (-1,1)") public int value;
}
