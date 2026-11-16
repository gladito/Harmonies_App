package com.example.demo.Models;
import jakarta.persistence.*;
@Entity @Table(name="memberships",uniqueConstraints=@UniqueConstraint(columnNames={"group_id","user_id"}))
public class Membership extends BaseEntity {
@ManyToOne(optional=false) @JoinColumn(name="group_id") public Group group;
@ManyToOne(optional=false) @JoinColumn(name="user_id") public User user;
@Column(length=80,nullable=false) public String instrument = "Por definir";
public boolean canPublish;
public boolean canApprove;
}
