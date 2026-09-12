package com.example.demo.Models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profiles")
public class Profile extends BaseEntity {

    @Column(length = 160)
    private String bio;

    private String avatarUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User owner;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    public Profile() {
    }

    public Profile(User owner) {
        this.owner = owner;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
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

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void addPost(Post post) {
        if (post != null && !posts.contains(post)) {
            posts.add(post);
            post.setProfile(this);
        }
    }

    public void removePost(Post post) {
        if (post != null && posts.remove(post)) {
            post.setProfile(null);
        }
    }
}

