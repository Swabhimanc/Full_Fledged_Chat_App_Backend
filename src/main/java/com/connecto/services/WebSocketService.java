package com.connecto.services;

import com.connecto.model.FriendRequest;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface WebSocketService {
    Map<String, Object> acceptFriendRequest(Map<String,Object> request) throws ExecutionException, InterruptedException;

    Map<String,Object> newFriendRequest(String from, String to) throws ExecutionException, InterruptedException;

    Map<String, Object> deleteFriendRequest(String from, String to) throws ExecutionException, InterruptedException;

    Map<String, Object> removeFriend(Map<String, Object> request) throws ExecutionException, InterruptedException;
}
