package com.connecto.DTO.responseDTO;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;

import java.util.Date;
import java.util.List;

public class AudioCallResponseDTO {
    private String id;
    private List<String> participants;
    private UserResponseDTO from;
    private UserResponseDTO to;
    private Verdict verdict;
    private Status status;
    private Date startedAt;
    private Date endedAt;

    public String getId() {
        return id;
    }

    public AudioCallResponseDTO setId(String id) {
        this.id = id;
        return this;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public AudioCallResponseDTO setParticipants(List<String> participants) {
        this.participants = participants;
        return this;
    }

    public UserResponseDTO getTo() {
        return to;
    }

    public AudioCallResponseDTO setTo(UserResponseDTO to) {
        this.to = to;
        return this;
    }

    public UserResponseDTO getFrom() {
        return from;
    }

    public AudioCallResponseDTO setFrom(UserResponseDTO from) {
        this.from = from;
        return this;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public AudioCallResponseDTO setVerdict(Verdict verdict) {
        this.verdict = verdict;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public AudioCallResponseDTO setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public AudioCallResponseDTO setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public AudioCallResponseDTO setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
        return this;
    }
}
