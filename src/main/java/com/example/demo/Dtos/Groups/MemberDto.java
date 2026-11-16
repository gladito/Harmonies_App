package com.example.demo.Dtos.Groups;

public class MemberDto {

    private Long id;
    private String username;
    private boolean owner;
    private String avatarUrl;

    public MemberDto() {
    }

    public MemberDto(Long id, String username, boolean owner) {
        this.id = id;
        this.username = username;
        this.owner = owner;
    }

    public MemberDto(Long id, String username, boolean owner, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.owner = owner;
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }
}


