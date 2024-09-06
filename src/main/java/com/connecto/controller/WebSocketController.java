package com.connecto.controller;

import com.connecto.enums.MessageType;
import com.connecto.enums.Status;
import com.connecto.model.FriendRequest;
import com.connecto.model.Message;
import com.connecto.model.User;
import com.connecto.services.MessageService;
import com.connecto.services.UserService;
import com.connecto.services.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate template;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    public WebSocketController(SimpMessagingTemplate template) {
        this.template = template;
    }

    //The client should send message to /app/friend-request
    //The client should subscribe to /user/${user_id}/topic/friend_request

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

    @MessageMapping("/send_friend_request")
    public void newFriendRequest(@Payload Map<String,Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        String from = payload.get("from").toString();
        String to = payload.get("to").toString();

        Map<String,Object> result = webSocketService.newFriendRequest(from,to);
        if(result.get("status").equals("success")){
            template.convertAndSendToUser(from,"/topic/request_sent",result);
            template.convertAndSendToUser(to,"/topic/new_friend_request","Friend Request Received from "+user.getFirstName());
        }else{
            template.convertAndSendToUser(from,"/topic/request_sent",result);
        }
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

    @MessageMapping("/get_direct_conversations")
    public void getDirectConversation(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");

        Map<String, Object> response = messageService.allDirectConversations(payload.get("user_id").toString());
        template.convertAndSendToUser(user.getId(), "/topic/get_direct_conversations", response);
    }

    @MessageMapping("/start_conversation")
    public void startConversation(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        String from = payload.get("from").toString();
        String to = payload.get("to").toString();

        Map<String, Object> response = messageService.startConversation(from, to);
        template.convertAndSendToUser(user.getId(), "/topic/start_conversation", response);
    }

    @MessageMapping("/get_messages")
    public void getMessages(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        List<Message> messages = messageService.getOneToOneMessages(payload.get("id").toString());
        template.convertAndSendToUser(user.getId(), "/topic/get_messages", messages);
    }

    @MessageMapping("/text_message")
    public void textMessages(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        String conversation_id = payload.get("conversation_id").toString();
        String to = payload.get("to").toString();
        String from = payload.get("from").toString();
        String type = payload.get("type").toString();
        String msg = payload.get("msg").toString();

        Message message = new Message()
                .setFrom(from)
                .setTo(to)
                .setText(msg)
                .setType(MessageType.valueOf(type));

        messageService.addMessage(conversation_id,message);
//        template.convertAndSendToUser(from,"/topic/text_message",message);
        template.convertAndSendToUser(to,"/topic/text_message",new HashMap<>(){{
            put("conversation_id",conversation_id);
            put("message",message);
        }});
        template.convertAndSendToUser(from,"/topic/text_message",new HashMap<>(){{
            put("conversation_id",conversation_id);
            put("message",message);
        }});
    }

    @MessageMapping("/file_message")
    public void fileMessages(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
    }
}
