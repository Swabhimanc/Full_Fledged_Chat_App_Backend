package com.connecto.services;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.model.Avatar;
import org.apache.commons.mail.EmailException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public interface AuthService {
    Map<String, Object> register(Map<String,Object> object) throws ExecutionException, InterruptedException, EmailException, IOException;

    Map<String,Object> login(Map<String,Object> object) throws ExecutionException, InterruptedException;

    void logout(String userId) throws ExecutionException, InterruptedException;

    Object setAvatar(Avatar avatarImage) throws ExecutionException, InterruptedException;

    List<UserResponseDTO> getAllUsers(String currentUserId) throws ExecutionException, InterruptedException;

    Map<String,Object> forgotPassword(Map<String,Object> object,String URL) throws ExecutionException, InterruptedException, EmailException;

    Map<String, Object> resetPassword(Object object) throws ExecutionException, InterruptedException, EmailException;
}
