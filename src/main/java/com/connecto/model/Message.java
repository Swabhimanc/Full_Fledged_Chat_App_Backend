package com.connecto.model;

import com.connecto.enums.MessageType;

import java.util.Date;

public class Message {
    private String to;
    private String from;
    private MessageType type;
    private Date createdAt = new Date();
    private String text;
    private String media;
    private String mediaType;

    public String getMediaType() {
        return mediaType;
    }

    public Message setMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public String getMedia() {
        return media;
    }

    public Message setMedia(String media) {
        this.media = media;
        return this;
    }

    public String getText() {
        return text;
    }

    public Message setText(String text) {
        this.text = text;
        return this;
    }

    public String getTo() {
        return to;
    }

    public Message setTo(String to) {
        this.to = to;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public Message setFrom(String from) {
        this.from = from;
        return this;
    }

    public MessageType getType() {
        return type;
    }

    public Message setType(MessageType type) {
        this.type = type;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Message setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
