package com.connecto.model;

import com.connecto.enums.MessageType;

import java.util.Date;

public class Message {
    private String to;
    private String from;
    private Enum<MessageType> type;
    private Date createdAt = new Date();
    private String text;
    private String file;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Enum<MessageType> getType() {
        return type;
    }

    public void setType(Enum<MessageType> type) {
        this.type = type;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
