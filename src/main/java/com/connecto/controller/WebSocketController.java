package com.connecto.controller;

import com.connecto.enums.Status;
import com.connecto.model.FriendRequest;
import com.connecto.model.Message;
import com.connecto.model.User;
import com.connecto.services.UserService;
import com.connecto.services.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate template;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private UserService userService;

    public WebSocketController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/send-message")
//    @SendTo("/topic/message-receive")
    public void sendMessage(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        template.convertAndSendToUser("", "/topic/message-receive/" + message.getTo(), message);
    }

    @MessageMapping("/add-user")//app/add-user
    @SendTo("/topic/public")
    public String addUser(@Payload String userId, SimpMessageHeaderAccessor headerAccessor) {
        return "...";
    }

    @MessageMapping("/new-friend-request") //The client should send message to /app/friend-request
    //The client should subscribe to /user/${user_id}/topic/friend_request
    public void friendRequest(@Payload FriendRequest request, SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        template.convertAndSendToUser(request.getRecipient(), "/topic/friend-request", "New Friend request received");
        template.convertAndSendToUser(request.getSender(), "/topic/friend-request", "Request sent successfully");
    }

    @MessageMapping("/accept-request")
    public void acceptRequest(@Payload FriendRequest request, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        //TODO When the accept request is received.
        //1. We are getting the current user object from the interceptor.
        //2. In the friends list of both the Sender and Recipient User, add the new reference.
        try {
            Map<String, Object> response = webSocketService.acceptFriendRequest(request);
            if ((boolean) response.get("status")) {
                template.convertAndSendToUser(request.getRecipient(), "/topic/request-accepted", "Friend request accepted");
                template.convertAndSendToUser(request.getSender(), "/topic/request-accepted", "Friend request accepted");
            }
        } catch (Exception e) {

        }
    }

    @MessageMapping("/end")
    public void connectionEnd(SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        try {
            userService.setUserStatus(user.getId(), Status.OFFLINE);
            template.convertAndSend("/topic/request-accepted", "");
        } catch (Exception e) {

        }
    }

    @MessageMapping("/text_message")
    public void textMessages(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        System.out.println("Received Message"+message.getText());
    }

    @MessageMapping("/file_message")
    public void fileMessages(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        System.out.println("Received Message"+message.getText());
    }
}
