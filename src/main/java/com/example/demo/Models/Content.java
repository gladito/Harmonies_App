package com.example.demo.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "contents")
@Inheritance(strategy = InheritanceType.JOINED)
public class Content extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PUBLIC;

    public Content() {
    }

    public Content(String title, String description, Visibility visibility) {
        this.title = title;
        this.description = description;
        if (visibility != null) {
            this.visibility = visibility;
        }
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

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    public String getContentType() {
        return "CONTENT";
    }
}

