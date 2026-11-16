package com.example.demo.Models;
import jakarta.persistence.*;
@Entity @Table(name="profile_ratings",uniqueConstraints=@UniqueConstraint(columnNames={"profile_id","reviewer_id"}))
public class ProfileRating extends BaseEntity {
@ManyToOne(optional=false) @JoinColumn(name="profile_id") public Profile profile;
@ManyToOne(optional=false) @JoinColumn(name="reviewer_id") public User reviewer;
@org.hibernate.annotations.Check(constraints="score between 1 and 5")
@Column(nullable=false) public int score;
}
