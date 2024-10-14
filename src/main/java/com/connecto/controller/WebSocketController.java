package com.connecto.controller;

import com.connecto.enums.MessageType;
import com.connecto.enums.Status;
import com.connecto.model.Message;
import com.connecto.model.User;
import com.connecto.services.MessageService;
import com.connecto.services.UserService;
import com.connecto.services.WebSocketService;
import com.connecto.socketIO.SocketIOConfig;
import com.connecto.socketIO.SocketIOService;
import com.connecto.utilities.security.JwtUtil;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate template;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    private WebSocketService webSocketService;
    @Autowired
    private SocketIOServer server;
    @Autowired
    private UserService userService;
    @Autowired
    private SocketIOService socketIOService;

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
        template.convertAndSendToUser(from, "/topic/notification", result.get("message"));
        SocketIOConfig.clientMap.get(from).sendEvent("notification", result.get("message"));
        if (result.get("status").equals("success")) {
            template.convertAndSendToUser(to, "/topic/notification", "Friend Request Received from " + user.getFirstName());
            SocketIOConfig.clientMap.get(to).sendEvent("notification", "New Friend Request Received");
        }
    }

    @MessageMapping("/cancel_friend_request")
    public void cancelFriendRequest(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        String from = payload.get("from").toString();
        String to = payload.get("to").toString();

        Map<String, Object> result = webSocketService.deleteFriendRequest(from, to);
        template.convertAndSendToUser(from, "/topic/notification", result.get("message"));
        SocketIOConfig.clientMap.get(from).sendEvent("notification", result.get("message"));
    }

    @MessageMapping("/accept_request")
    public void acceptRequest(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        String sender_id = payload.get("sender_id").toString();
        String recipient_id = payload.get("recipient_id").toString();
        try {
            Map<String, Object> response = webSocketService.acceptFriendRequest(payload);
            if ((boolean) response.get("status")) {
                template.convertAndSendToUser(sender_id, "/topic/notification", user.getFirstName() + " accepted your Friend Request");
                SocketIOConfig.clientMap.get(sender_id).sendEvent("notification", user.getFirstName() + " accepted your Friend Request");
            } else {
                template.convertAndSendToUser(recipient_id, "/topic/notification", "Failed to Accept");
                SocketIOConfig.clientMap.get(recipient_id).sendEvent("notification", "Failed to Accept");
            }
        } catch (Exception e) {
            template.convertAndSendToUser(recipient_id, "/topic/notification", e.getMessage());
            SocketIOConfig.clientMap.get(recipient_id).sendEvent("notification", e.getMessage());
        }
    }

    @MessageMapping("/remove_friend")
    public void removeFriend(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        String from = payload.get("from").toString();
        String to = payload.get("to").toString();
        try {
            Map<String, Object> response = webSocketService.removeFriend(payload);
            template.convertAndSendToUser(from, "/topic/notification", response.get("message"));
            SocketIOConfig.clientMap.get(from).sendEvent("notification", response.get("message"));
        } catch (Exception e) {
            template.convertAndSendToUser(from, "/topic/notification", "Something went wrong");
            SocketIOConfig.clientMap.get(from).sendEvent("notification", "Something went wrong");
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
        String userId = payload.get("user_id").toString();
        Map<String, Object> response = messageService.allDirectConversations(payload.get("user_id").toString());
        template.convertAndSendToUser(user.getId(), "/topic/get_direct_conversations", response);
        SocketIOConfig.clientMap.get(userId).sendEvent("get_direct_conversations", response);
    }

    @MessageMapping("/start_conversation")
    public void startConversation(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        User user = (User) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("user");
        String from = payload.get("from").toString();
        String to = payload.get("to").toString();

        Map<String, Object> response = messageService.startConversation(from, to);
        template.convertAndSendToUser(user.getId(), "/topic/start_conversation", response);
        SocketIOConfig.clientMap.get(from).sendEvent("start_conversation", response);
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
        socketIOService.sendToUser(from, "new_message", new HashMap<>() {{
            put("conversation_id", conversation_id);
            put("message", message);
        }});
        socketIOService.sendToUser(to, "new_message", new HashMap<>() {{
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
        SocketIOConfig.clientMap.get(from).sendEvent("new_message", new HashMap<>() {{
            put("conversation_id", conversation_id);
            put("message", message);
        }});
        SocketIOConfig.clientMap.get(to).sendEvent("new_message", new HashMap<>() {{
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
        String message_id = payload.get("id") == null ? "" : payload.get("id").toString();
        String conversation_id = payload.get("room_id") == null ? "" : payload.get("room_id").toString();
        String user_id = payload.get("user_id") == null ? "" : payload.get("user_id").toString();
        try {
            HashMap<String, Object> response = messageService.deleteMessage(conversation_id, message_id, user_id).get();
            template.convertAndSendToUser(user_id, "/topic/delete_message", response);
            SocketIOConfig.clientMap.get(user_id).sendEvent("delete_message", response);
        } catch (Exception e) {
            template.convertAndSendToUser(user_id, "/topic/delete_message", new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }});
            SocketIOConfig.clientMap.get(user_id).sendEvent("delete_message", new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }});
        }
    }

    @PostConstruct
    public void setupEventListeners() {
        server.addEventListener("send_friend_request", Map.class, ((client, payload, ackRequest) -> {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();

            Map<String, Object> result = webSocketService.newFriendRequest(from, to);
            socketIOService.sendToUser(from, "notification", result.get("message"));
            template.convertAndSendToUser(from, "/topic/notification", result.get("message"));
            if (result.get("status").equals("success")) {
                socketIOService.sendToUser(to, "notification", "New Friend Request Received");
                template.convertAndSendToUser(to, "/topic/notification", "New Friend Request Received");
            }
        }));

        server.addEventListener("cancel_friend_request", Map.class, ((client, payload, ackRequest) -> {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();

            Map<String, Object> result = webSocketService.deleteFriendRequest(from, to);
            socketIOService.sendToUser(from, "notification", result.get("message"));
            template.convertAndSendToUser(from, "/topic/notification", result.get("message"));
        }));

        server.addEventListener("accept_request", Map.class, ((client, payload, ackRequest) -> {
            String sender_id = payload.get("sender_id").toString();
            String recipient_id = payload.get("recipient_id").toString();
            try {
                Map<String, Object> response = webSocketService.acceptFriendRequest(payload);
                if ((boolean) response.get("status")) {
                    socketIOService.sendToUser(sender_id, "notification", recipient_id + " accepted your Friend Request");
                    template.convertAndSendToUser(sender_id, "/topic/notification", recipient_id + " accepted your Friend Request");
                } else {
                    socketIOService.sendToUser(recipient_id, "notification", "Failed to Accept");
                    template.convertAndSendToUser(recipient_id, "/topic/notification", "Failed to Accept");
                }
            } catch (Exception e) {
                socketIOService.sendToUser(recipient_id, "notification", e.getMessage());
                template.convertAndSendToUser(recipient_id, "/topic/notification", e.getMessage());
            }
        }));

        server.addEventListener("remove_friend", Map.class, ((client, payload, ackRequest) -> {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            try {
                Map<String, Object> response = webSocketService.removeFriend(payload);
                socketIOService.sendToUser(from, "notification", response.get("message"));
                template.convertAndSendToUser(from, "/topic/notification", response.get("message"));
            } catch (Exception e) {
                socketIOService.sendToUser(from, "notification", "Something went wrong");
                template.convertAndSendToUser(from, "/topic/notification", "Something went wrong");
            }
        }));

        server.addEventListener("end", Map.class, ((client, payload, ackRequest) -> {
            try {
                HandshakeData handshakeData = client.getHandshakeData();
                String token = handshakeData.getSingleUrlParam("token");
                if (handshakeData.getUrlParams().containsKey("token")) {
                    token = handshakeData.getUrlParams().get("token").get(0);
                }
                token = token.substring(7);
                String userId = jwtUtil.extractUserId(token);
                userService.setUserStatus(userId, Status.OFFLINE);
            } catch (ExecutionException e) {

            }
        }));

        server.addEventListener("get_direct_conversations", Map.class, ((client, payload, ackRequest) -> {
            String userId = payload.get("user_id").toString();
            Map<String, Object> response = messageService.allDirectConversations(userId);
            socketIOService.sendToUser(userId, "get_direct_conversations", response);
            template.convertAndSendToUser(userId, "/topic/get_direct_conversations", response);
        }));

        server.addEventListener("start_conversation", Map.class, ((client, payload, ackRequest) -> {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();

            Map<String, Object> response = messageService.startConversation(from, to);
            socketIOService.sendToUser(from, "start_conversation", response);
            template.convertAndSendToUser(from, "/topic/start_conversation", response);
        }));

        server.addEventListener("get_messages", Map.class, ((client, payload, ackRequest) -> {
            //TODO Not in use
        }));

        server.addEventListener("text_message", Map.class, ((client, payload, ackRequest) -> {
            String conversation_id = payload.get("conversation_id").toString();
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            String type = payload.get("type").toString();
            String msg = payload.get("message").toString();

            Message message = new Message().setFrom(from).setTo(to).setText(msg).setType(MessageType.valueOf(type));

            messageService.addMessage(conversation_id, message);
            socketIOService.sendToUser(from, "new_message", new HashMap<>() {{
                put("conversation_id", conversation_id);
                put("message", message);
            }});
            socketIOService.sendToUser(to, "new_message", new HashMap<>() {{
                put("conversation_id", conversation_id);
                put("message", message);
            }});
//            template.convertAndSendToUser(to, "/topic/new_message", new HashMap<>() {{
//                put("conversation_id", conversation_id);
//                put("message", message);
//            }});
//            template.convertAndSendToUser(from, "/topic/new_message", new HashMap<>() {{
//                put("conversation_id", conversation_id);
//                put("message", message);
//            }});
        }));

        server.addEventListener("media_message", Map.class, ((client, payload, ackRequest) -> {
            String conversation_id = payload.get("conversation_id").toString();
            String to = payload.get("to").toString();
            String from = payload.get("from").toString();
            String type = payload.get("type").toString();
            String msg = payload.get("message").toString();
            String media = payload.get("media").toString();
            String mediaType = payload.get("mediaType").toString();

            Message message = new Message().setFrom(from).setTo(to).setText(msg).setMedia(media).setMediaType(mediaType).setType(MessageType.valueOf(type));

            messageService.addMessage(conversation_id, message);

            socketIOService.sendToUser(from, "new_message", new HashMap<>() {{
                put("conversation_id", conversation_id);
                put("message", message);
            }});
            socketIOService.sendToUser(to, "new_message", new HashMap<>() {{
                put("conversation_id", conversation_id);
                put("message", message);
            }});
            template.convertAndSendToUser(to, "/topic/new_message", new HashMap<>() {{
                put("conversation_id", conversation_id);
                put("message", message);
            }});
            template.convertAndSendToUser(from, "/topic/new_message", new HashMap<>() {{
                put("conversation_id", conversation_id);
                put("message", message);
            }});
        }));

        server.addEventListener("file_message", Map.class, ((client, payload, ackRequest) -> {

        }));

        server.addEventListener("read_messages", Map.class, ((client, payload, ackRequest) -> {
            String from = payload.get("user_id").toString();
            String conversation_id = payload.get("room_id").toString();
            messageService.resetUnreadCount(from, conversation_id);
        }));

        server.addEventListener("delete_message", Map.class, ((client, payload, ackRequest) -> {
            String message_id = payload.get("id") == null ? "" : payload.get("id").toString();
            String conversation_id = payload.get("room_id") == null ? "" : payload.get("room_id").toString();
            String user_id = payload.get("user_id") == null ? "" : payload.get("user_id").toString();
            try {
                HashMap<String, Object> response = messageService.deleteMessage(conversation_id, message_id, user_id).get();
                socketIOService.sendToUser(user_id, "delete_message", response);
                template.convertAndSendToUser(user_id, "/topic/delete_message", response);
            } catch (Exception e) {
                socketIOService.sendToUser(user_id, "delete_message", new HashMap<>() {{
                    put("status", false);
                    put("message", e.getMessage());
                }});
                template.convertAndSendToUser(user_id, "/topic/delete_message", new HashMap<>() {{
                    put("status", false);
                    put("message", e.getMessage());
                }});
            }
        }));

    }
}