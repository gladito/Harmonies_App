package com.example.demo.Models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "person_id", nullable = false, unique = true)
    private Person person;
    public Person getPerson() { return person; }
    public String getFirstName() { return person.getFirstName(); }
    public String getLastName() { return person.getLastName(); }
    public LocalDate getBirthDate() { return person.getBirthDate(); }


    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToOne(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Profile profile;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "owner")
    private Set<Group> ownedGroups = new HashSet<>();

    @ManyToMany(mappedBy = "members")
    private Set<Group> groups = new HashSet<>();

    public User() {
    }

    public User(String firstName, String lastName, LocalDate birthDate, String username, String email, String password) {
        this.person = new Person(firstName, lastName, birthDate);
        assignProfile(new Profile(this));
        this.username = Objects.requireNonNull(username, "username es obligatorio");
        this.email = Objects.requireNonNull(email, "email es obligatorio");
        setPassword(Objects.requireNonNull(password, "password es obligatorio"));
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        Objects.requireNonNull(password, "password es obligatorio");
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Profile getProfile() {
        return profile;
    }

    public void assignProfile(Profile profile) {
        if (profile != null) {
            profile.setOwner(this);
        }
        this.profile = profile;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        if (role != null) {
            roles.add(role);
        }
    }

    public void removeRole(Role role) {
        if (role != null) {
            roles.remove(role);
        }
    }

    public Set<Group> getOwnedGroups() {
        return ownedGroups;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void addOwnedGroup(Group group) {
        if (group != null) {
            ownedGroups.add(group);
        }
    }

    public void removeOwnedGroup(Group group) {
        if (group != null) {
            ownedGroups.remove(group);
        }
    }

    public void addGroup(Group group) {
        if (group != null) {
            groups.add(group);
        }
    }

    public void removeGroup(Group group) {
        if (group != null) {
            groups.remove(group);
        }
    }

}

