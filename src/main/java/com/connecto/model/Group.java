package com.connecto.model;

import com.connecto.DTO.responseDTO.UserResponseDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Group {
    private String id;
    private String groupName;
    private String groupAvatar;
    private UserResponseDTO owner;
    private String ownerId;
    private List<UserResponseDTO> participants;
    private Map<String, Long> unreadCounts;
    private String lastMessage;
    private Date lastMessageTime;
    private Date createdAt;
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public Group setId(String id) {
        this.id = id;
        return this;
    }

    public String getGroupName() {
        return groupName;
    }

    public Group setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public String getGroupAvatar() {
        return groupAvatar;
    }

    public Group setGroupAvatar(String groupAvatar) {
        this.groupAvatar = groupAvatar;
        return this;
    }

    public UserResponseDTO getOwner() {
        return owner;
    }

    public Group setOwner(UserResponseDTO owner) {
        this.owner = owner;
        return this;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Group setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public List<UserResponseDTO> getParticipants() {
        return participants;
    }

    public Group setParticipants(List<UserResponseDTO> participants) {
        this.participants = participants;
        return this;
    }

    public Map<String, Long> getUnreadCounts() {
        return unreadCounts;
    }

    public Group setUnreadCounts(Map<String, Long> unreadCounts) {
        this.unreadCounts = unreadCounts;
        return this;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public Group setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
        return this;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public Group setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Group setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Group setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
