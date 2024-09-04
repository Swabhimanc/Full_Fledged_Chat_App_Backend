package com.connecto.model;

import com.connecto.DTO.responseDTO.UserResponseDTO;

import java.util.List;

public class OneToOneMessage {
    private String id;
    private List<UserResponseDTO> participants;
    private List<Message> messages;

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
