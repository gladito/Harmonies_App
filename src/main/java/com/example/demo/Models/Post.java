package com.example.demo.Models;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "posts")
public class Post extends Content {

    @Column(nullable = false)
    private String audioUrl;

    @Column
    private Integer durationInSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private Profile profile;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="band_id")
    private Group band;
    public Group getBand() { return band; }
    public void setBand(Group band) { this.band = band; }


    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private Set<Comment> comments = new LinkedHashSet<>();

    public Post() {
    }

    public Post(String title, String description, Visibility visibility, String audioUrl, Integer durationInSeconds, User author) {
        super(title, description, visibility);
        this.audioUrl = Objects.requireNonNull(audioUrl, "audioUrl es obligatorio");
        this.durationInSeconds = durationInSeconds;
        this.author = Objects.requireNonNull(author, "author es obligatorio");
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Integer getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(Integer durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public void addComment(Comment comment) {
        if (comment != null) {
            comments.add(comment);
        }
    }

    public void removeComment(Comment comment) {
        if (comment != null) {
            comments.remove(comment);
        }
    }

    public void addTag(Tag tag) {
        if (tag != null) {
            tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        if (tag != null) {
            tags.remove(tag);
        }
    }

    @Override
    public String getContentType() {
        return "POST";
    }
}

