package com.connecto.model;

import java.util.Date;

public class Message {
    private String from;
    private String to;
    private String message;
    private Date createdAt;
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Constructors, getters, setters, and validation logic

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    // Validate method
    public static boolean validate(Message message) {
        return message.getFrom() != null && message.getTo() != null && message.getMessage() != null;
    }
}
