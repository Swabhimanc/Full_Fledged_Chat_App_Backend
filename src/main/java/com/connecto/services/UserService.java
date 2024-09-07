package com.connecto.services;

import com.connecto.enums.Status;
import com.connecto.model.User;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface UserService {
    Map<String, Object> getAllUsers(User user) throws ExecutionException, InterruptedException;

    Map<String, Object> getFriendRequests(User user) throws ExecutionException, InterruptedException;

    void setUserStatus(String userId, Status status) throws ExecutionException, InterruptedException;

    Map<String, Object> getFriends(User user) throws ExecutionException, InterruptedException;
}
