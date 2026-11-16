package com.example.demo.Dtos.Groups;

public class GroupSummaryDto {

    private Long id;
    private String name;
    private String description;
    private String avatarUrl;
    private int memberCount;
    private java.util.List<MemberDto> members;
    private boolean owner;

    public GroupSummaryDto() {
    }

    public GroupSummaryDto(Long id, String name, String description, int memberCount, boolean owner) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.memberCount = memberCount;
        this.owner = owner;
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

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public java.util.List<MemberDto> getMembers() {
        return members;
    }

    public void setMembers(java.util.List<MemberDto> members) {
        this.members = members;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }
}

