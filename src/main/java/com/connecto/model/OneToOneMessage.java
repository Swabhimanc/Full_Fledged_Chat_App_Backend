package com.connecto.model;

import com.connecto.DTO.responseDTO.UserResponseDTO;

import java.util.List;
import java.util.Map;

public class OneToOneMessage {
    private String id;
    private List<UserResponseDTO> participants;
    private List<Message> messages;
    private Map<String,Long> unreadCounts;

    public Map<String, Long> getUnreadCounts() {
        return unreadCounts;
    }

    public OneToOneMessage setUnreadCounts(Map<String, Long> unreadCounts) {
        this.unreadCounts = unreadCounts;
        return this;
    }

    public String getId() {
        return id;
    }

    public OneToOneMessage setId(String id) {
        this.id = id;
        return this;
    }

    public List<UserResponseDTO> getParticipants() {
        return participants;
    }

    public OneToOneMessage setParticipants(List<UserResponseDTO> participants) {
        this.participants = participants;
        return this;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public OneToOneMessage setMessages(List<Message> messages) {
        this.messages = messages;
        return this;
    }
}
