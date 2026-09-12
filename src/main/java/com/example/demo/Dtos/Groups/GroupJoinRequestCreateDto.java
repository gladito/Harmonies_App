package com.example.demo.Dtos.Groups;

public class GroupJoinRequestCreateDto {

    private Long groupId;
    private String message;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}