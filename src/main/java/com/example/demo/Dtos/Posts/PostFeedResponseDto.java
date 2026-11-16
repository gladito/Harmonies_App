package com.example.demo.Dtos.Posts;

import java.time.LocalDateTime;

public class PostFeedResponseDto {
    private java.util.List<String> tags=java.util.List.of();
    public java.util.List<String> getTags(){return tags;} public void setTags(java.util.List<String> tags){this.tags=tags;}
    private Long authorId; private String audioUrl; private String bandName;
    private String authorAvatarUrl; private String bandAvatarUrl;
    public Long getAuthorId(){return authorId;} public void setAuthorId(Long id){authorId=id;}
    public String getAudioUrl(){return audioUrl;} public void setAudioUrl(String s){audioUrl=s;}
    public String getBandName(){return bandName;} public void setBandName(String s){bandName=s;}
    public String getAuthorAvatarUrl(){return authorAvatarUrl;} public void setAuthorAvatarUrl(String s){authorAvatarUrl=s;}
    public String getBandAvatarUrl(){return bandAvatarUrl;} public void setBandAvatarUrl(String s){bandAvatarUrl=s;}


    private Long id;
    private String title;
    private String description;
    private String authorUsername;
    private LocalDateTime createdAt;

    public PostFeedResponseDto() {
    }

    public PostFeedResponseDto(Long id, String title, String description, String authorUsername, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


