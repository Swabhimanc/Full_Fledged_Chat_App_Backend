package com.connecto.controller;

import com.connecto.enums.MessageType;
import com.connecto.enums.Status;
import com.connecto.model.Message;
import com.connecto.model.User;
import com.connecto.services.MessageService;
import com.connecto.services.UserService;
import com.connecto.services.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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

    @MessageMapping("/send_friend_request")
    public void newFriendRequest(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        String from = payload.get("from").toString();
        String to = payload.get("to").toString();

        Map<String, Object> result = webSocketService.newFriendRequest(from, to);
        if (result.get("status").equals("success")) {
            template.convertAndSendToUser(from, "/topic/notification", result.get("message"));
            template.convertAndSendToUser(to, "/topic/notification", "Friend Request Received from " + user.getFirstName());
        } else {
            template.convertAndSendToUser(from, "/topic/notification", result.get("message"));
        }
    }

    @MessageMapping("/cancel_friend_request")
    public void cancelFriendRequest(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        String from = payload.get("from").toString();
        String to = payload.get("to").toString();

        Map<String, Object> result = webSocketService.deleteFriendRequest(from, to);
        template.convertAndSendToUser(from, "/topic/notification", result.get("message"));
    }

    @MessageMapping("/accept_request")
    public void acceptRequest(@Payload Map<String, Object> request, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        try {
            Map<String, Object> response = webSocketService.acceptFriendRequest(request);
            if ((boolean) response.get("status")) {
                template.convertAndSendToUser(request.get("sender_id").toString(), "/topic/notification", user.getFirstName() + " accepted your Friend Request");
            } else {
                template.convertAndSendToUser(user.getId(), "/topic/notification", "Failed to Accept");
            }
        } catch (Exception e) {
            template.convertAndSendToUser(user.getId(), "/topic/notification", e.getMessage());
        }
    }

    @MessageMapping("/remove_friend")
    public void removeFriend(@Payload Map<String, Object> request, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        try {
            Map<String, Object> response = webSocketService.removeFriend(request);
            template.convertAndSendToUser(request.get("from").toString(), "/topic/notification", response.get("message"));
        } catch (Exception e) {
            template.convertAndSendToUser(user.getId(), "/topic/notification", "Something went wrong");
        }
    }

    @MessageMapping("/end")
    public void connectionEnd(@Payload Map<String, Object> request, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        try {
            userService.setUserStatus(user.getId(), Status.OFFLINE);
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

    //Not In Use
    @MessageMapping("/get_messages")
    public void getMessages(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        List<Message> messages = messageService.getOneToOneMessages(payload.get("conversation_id").toString());
        template.convertAndSendToUser(user.getId(), "/topic/get_messages", messages);
    }

    @MessageMapping("/text_message")
    public void textMessages(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        String conversation_id = payload.get("conversation_id").toString();
        String from = payload.get("from").toString();
        String to = payload.get("to").toString();
        String type = payload.get("type").toString();
        String msg = payload.get("message").toString();

        Message message = new Message().setFrom(from).setTo(to).setText(msg).setType(MessageType.valueOf(type));

        messageService.addMessage(conversation_id, message);
        template.convertAndSendToUser(to, "/topic/new_message", new HashMap<>() {{
            put("conversation_id", conversation_id);
            put("message", message);
        }});
        template.convertAndSendToUser(from, "/topic/new_message", new HashMap<>() {{
            put("conversation_id", conversation_id);
            put("message", message);
        }});
    }

    @MessageMapping("/media_message")
    public void mediaMessages(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        String conversation_id = payload.get("conversation_id").toString();
        String to = payload.get("to").toString();
        String from = payload.get("from").toString();
        String type = payload.get("type").toString();
        String msg = payload.get("message").toString();
        String media = payload.get("media").toString();
        String mediaType = payload.get("mediaType").toString();

        Message message = new Message().setFrom(from).setTo(to).setText(msg).setMedia(media).setMediaType(mediaType).setType(MessageType.valueOf(type));

        messageService.addMessage(conversation_id, message);
        template.convertAndSendToUser(to, "/topic/new_message", new HashMap<>() {{
            put("conversation_id", conversation_id);
            put("message", message);
        }});
        template.convertAndSendToUser(from, "/topic/new_message", new HashMap<>() {{
            put("conversation_id", conversation_id);
            put("message", message);
        }});
    }

    @MessageMapping("/file_message")
    public void fileMessages(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
    }

    @MessageMapping("/read_messages")
    public void readMessages(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        String from = payload.get("user_id").toString();
        String conversation_id = payload.get("room_id").toString();
        messageService.resetUnreadCount(from, conversation_id);
    }

    @MessageMapping("/delete_message")
    public void deleteMessage(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        try {
            String message_id = payload.get("id") == null ? "" : payload.get("id").toString();
            String conversation_id = payload.get("room_id") == null ? "" : payload.get("room_id").toString();
            String user_id = payload.get("user_id") == null ? "" : payload.get("user_id").toString();
            HashMap<String, Object> response = messageService.deleteMessage(conversation_id, message_id, user_id).get();
            template.convertAndSendToUser(user.getId(), "/topic/delete_message", response);
        } catch (Exception e) {
            template.convertAndSendToUser(user.getId(), "/topic/delete_message", new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }});
        }
    }
}