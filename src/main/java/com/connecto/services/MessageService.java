package com.connecto.services;


import com.connecto.model.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface MessageService {

    Object addMessage(Message message) throws ExecutionException, InterruptedException;

    List<Map<?,?>> getAllMessages(String from, String to) throws ExecutionException, InterruptedException;

    Object getLimitedMessage(String from, String to, String lastVisible, Integer limit) throws ExecutionException, InterruptedException;

    Map<String,Object> allDirectConversations(String userId) throws ExecutionException, InterruptedException;

    Map<String, Object> startConversation(String from, String to) throws ExecutionException, InterruptedException;
}
