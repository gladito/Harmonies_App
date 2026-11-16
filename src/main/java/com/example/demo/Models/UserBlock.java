package com.example.demo.Models;
import jakarta.persistence.*;
@Entity @Table(name="user_blocks",uniqueConstraints=@UniqueConstraint(columnNames={"owner_id","target_id"}))
public class UserBlock extends BaseEntity {
@ManyToOne(optional=false) @JoinColumn(name="owner_id") public User owner;
@ManyToOne(optional=false) @JoinColumn(name="target_id") public User target;
}
