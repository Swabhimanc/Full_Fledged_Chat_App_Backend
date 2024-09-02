package com.connecto.model;

import java.util.List;

public class OneToOneMessage {
    private List<User> participants;
    private List<Message> messages;

    public List<User> getParticipants() {
        return participants;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
