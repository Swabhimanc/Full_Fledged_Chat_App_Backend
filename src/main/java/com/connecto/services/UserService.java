package com.connecto.services;

import com.connecto.model.User;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface UserService {
    Map<String, Object> getAllUsers(User user) throws ExecutionException, InterruptedException;

    Map<String, Object> getFriendRequests(User user) throws ExecutionException, InterruptedException;

}
