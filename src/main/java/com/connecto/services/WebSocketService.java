package com.connecto.services;

import com.connecto.model.FriendRequest;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface WebSocketService {
    Map<String, Object> acceptFriendRequest(FriendRequest request) throws ExecutionException, InterruptedException;
}
