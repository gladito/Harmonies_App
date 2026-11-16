package com.example.demo.Models;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "groups")
public class Group extends BaseEntity {

    @Version private long version;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    public Group() {
    }

    public Group(String name, String description, User owner) {
        this.name = Objects.requireNonNull(name, "El nombre del grupo es obligatorio");
        this.description = description;
        assignOwner(Objects.requireNonNull(owner, "El dueño del grupo es obligatorio"));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public User getOwner() {
        return owner;
    }

    public void assignOwner(User owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner no puede ser nulo");
        }
        if (this.owner != null) {
            this.owner.removeOwnedGroup(this);
        }
        this.owner = owner;
        owner.addOwnedGroup(this);
        addMember(owner);
    }

    public Set<User> getMembers() {
        return members;
    }

    public int getMemberCount() {
        return (int) members.stream().filter(member -> !member.isDeleted()).count();
    }

    public boolean isFull() {
        return false;
    }

    public boolean hasMember(User user) {
        return user != null && members.stream().anyMatch(m -> m == user || (m.getId() != null && m.getId().equals(user.getId())));
    }

    public void addMember(User user) {
        if (user == null) {
            return;
        }
        if (isFull() && !hasMember(user)) {
            throw new IllegalStateException("El grupo ha alcanzado el máximo de miembros");
        }
        if (!hasMember(user)) members.add(user);
        user.addGroup(this);
    }

    public void removeMember(User user) {
        if (user == null) {
            return;
        }
        if (members.remove(user)) {
            user.removeGroup(this);
        }
    }
}

