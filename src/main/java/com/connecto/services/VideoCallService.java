package com.connecto.services;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface VideoCallService {
    Map<String, Object> startVideoCall(String from, String to) throws ExecutionException, InterruptedException;

    Map<String,Object> startVideoCall(String from, String to, String roomID) throws ExecutionException, InterruptedException;

    void updateCallRecord(String to, String from, Verdict verdict, Status status) throws ExecutionException, InterruptedException;
}
