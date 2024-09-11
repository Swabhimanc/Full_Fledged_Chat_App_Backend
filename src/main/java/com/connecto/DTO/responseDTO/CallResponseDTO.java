package com.connecto.DTO.responseDTO;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;

import java.util.Date;
import java.util.List;

public class CallResponseDTO {
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

    public CallResponseDTO setId(String id) {
        this.id = id;
        return this;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public CallResponseDTO setParticipants(List<String> participants) {
        this.participants = participants;
        return this;
    }

    public UserResponseDTO getTo() {
        return to;
    }

    public CallResponseDTO setTo(UserResponseDTO to) {
        this.to = to;
        return this;
    }

    public UserResponseDTO getFrom() {
        return from;
    }

    public CallResponseDTO setFrom(UserResponseDTO from) {
        this.from = from;
        return this;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public CallResponseDTO setVerdict(Verdict verdict) {
        this.verdict = verdict;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public CallResponseDTO setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public CallResponseDTO setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public CallResponseDTO setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
        return this;
    }
}
