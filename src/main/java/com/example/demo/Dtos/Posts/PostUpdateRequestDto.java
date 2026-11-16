package com.example.demo.Dtos.Posts;

import com.example.demo.Models.Visibility;

public class PostUpdateRequestDto {

    private Long id;
    private String title;
    private String tags;
    public String getTags(){return tags;}
    public void setTags(String tags){this.tags=tags;}
    private String description;
    private String audioUrl;
    private Integer durationInSeconds;
    private Visibility visibility = Visibility.PUBLIC;

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

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
}


