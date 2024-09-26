package com.connecto.services;


import com.connecto.model.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface MessageService {

    void addMessage(String conversation_id, Message message) throws ExecutionException, InterruptedException;

    List<Map<?,?>> getAllMessages(String from, String to) throws ExecutionException, InterruptedException;

    Object getLimitedMessage(String from, String to, String lastVisible, Integer limit) throws ExecutionException, InterruptedException;

    Map<String,Object> allDirectConversations(String userId) throws ExecutionException, InterruptedException;

    Map<String, Object> startConversation(String from, String to) throws ExecutionException, InterruptedException;

    List<Message> getOneToOneMessages(String id) throws ExecutionException, InterruptedException;

    void resetUnreadCount(String from, String conversationId) throws ExecutionException, InterruptedException;

    CompletableFuture<HashMap<String, Object>> deleteMessage(String conversationId, String messageId, String userId) throws ExecutionException, InterruptedException;
}
