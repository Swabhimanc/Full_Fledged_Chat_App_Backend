package com.connecto.controller;

import com.connecto.enums.MessageType;
import com.connecto.enums.Status;
import com.connecto.model.Group;
import com.connecto.model.Message;
import com.connecto.model.User;
import com.connecto.services.GroupService;
import com.connecto.services.MessageService;
import com.connecto.services.UserService;
import com.connecto.services.WebSocketService;
import com.connecto.socketIO.SocketIOService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebSocketController {
    private final SocketIOServer server;
    private final SocketIOService socketIOService;
    private final WebSocketService webSocketService;
    private final UserService userService;
    private final MessageService messageService;
    private final GroupService groupService;

    public WebSocketController(
            SocketIOServer server,
            SocketIOService socketIOService,
            WebSocketService webSocketService,
            UserService userService,
            MessageService messageService,
            GroupService groupService
    ) {
        this.server = server;
        this.socketIOService = socketIOService;
        this.webSocketService = webSocketService;
        this.userService = userService;
        this.messageService = messageService;
        this.groupService = groupService;
    }

    @PostConstruct
    public void setupEventListeners() {
        server.addEventListener("send_friend_request", Map.class, (client, payload, ack) -> {
            User user = user(client);
            String to = required(payload, "to");
            Map<String, Object> result = webSocketService.newFriendRequest(user.getId(), to);
            socketIOService.sendToUser(user.getId(), "notification", result.get("message"));
            if ("success".equals(result.get("status"))) {
                socketIOService.sendToUser(to, "notification", "New Friend Request Received");
            }
        });

        server.addEventListener("cancel_friend_request", Map.class, (client, payload, ack) -> {
            User user = user(client);
            Map<String, Object> result = webSocketService.deleteFriendRequest(user.getId(), required(payload, "to"));
            socketIOService.sendToUser(user.getId(), "notification", result.get("message"));
        });

        server.addEventListener("accept_request", Map.class, (client, payload, ack) -> {
            User user = user(client);
            String senderId = required(payload, "sender_id");
            Map<String, Object> request = new HashMap<>(payload);
            request.put("recipient_id", user.getId());
            Map<String, Object> result = webSocketService.acceptFriendRequest(request);
            String message = Boolean.TRUE.equals(result.get("status"))
                    ? user.getFirstName() + " accepted your Friend Request"
                    : "Failed to Accept";
            socketIOService.sendToUser(Boolean.TRUE.equals(result.get("status")) ? senderId : user.getId(), "notification", message);
        });

        server.addEventListener("remove_friend", Map.class, (client, payload, ack) -> {
            User user = user(client);
            Map<String, Object> request = new HashMap<>(payload);
            request.put("from", user.getId());
            Map<String, Object> result = webSocketService.removeFriend(request);
            socketIOService.sendToUser(user.getId(), "notification", result.get("message"));
        });

        server.addEventListener("end", Map.class, (client, payload, ack) ->
                userService.setUserStatus(user(client).getId(), Status.OFFLINE));

        server.addEventListener("get_direct_conversations", Map.class, (client, payload, ack) -> {
            User user = user(client);
            socketIOService.sendToUser(user.getId(), "get_direct_conversations", messageService.allDirectConversations(user.getId()));
        });

        server.addEventListener("start_conversation", Map.class, (client, payload, ack) -> {
            User user = user(client);
            Map<String, Object> response = messageService.startConversation(user.getId(), required(payload, "to"));
            socketIOService.sendToUser(user.getId(), "start_conversation", response);
        });

        server.addEventListener("text_message", Map.class, (client, payload, ack) ->
                sendDirectMessage(client, payload, false));
        server.addEventListener("media_message", Map.class, (client, payload, ack) ->
                sendDirectMessage(client, payload, true));

        server.addEventListener("read_messages", Map.class, (client, payload, ack) ->
                messageService.resetUnreadCount(user(client).getId(), required(payload, "room_id")));

        server.addEventListener("delete_message", Map.class, (client, payload, ack) -> {
            User user = user(client);
            Map<String, Object> response;
            try {
                response = messageService.deleteMessage(
                        required(payload, "room_id"),
                        required(payload, "id"),
                        user.getId()
                ).get();
            } catch (Exception exception) {
                response = Map.of("status", false, "message", "Unable to delete message");
            }
            socketIOService.sendToUser(user.getId(), "delete_message", response);
        });

        server.addEventListener("create_group", Map.class, (client, payload, ack) -> {
            User user = user(client);
            List<String> members = payload.get("members") instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : new ArrayList<>();
            Map<String, Object> response = groupService.createGroup(
                    user.getId(),
                    optional(payload, "groupName"),
                    optional(payload, "groupAvatar"),
                    members
            );
            if (Boolean.TRUE.equals(response.get("status")) && response.get("data") instanceof Group group) {
                publishToParticipants(groupService.getParticipantIds(group.getId()), "group_created", response);
            } else {
                socketIOService.sendToUser(user.getId(), "group_created", response);
            }
        });

        server.addEventListener("group_text_message", Map.class, (client, payload, ack) ->
                sendGroupMessage(client, payload, false));
        server.addEventListener("group_media_message", Map.class, (client, payload, ack) ->
                sendGroupMessage(client, payload, true));

        server.addEventListener("read_group_messages", Map.class, (client, payload, ack) ->
                groupService.resetUnreadCount(user(client).getId(), required(payload, "group_id")));

        server.addEventListener("group_delete_message", Map.class, (client, payload, ack) -> {
            User user = user(client);
            String groupId = required(payload, "group_id");
            String messageId = required(payload, "id");
            Map<String, Object> response = groupService.deleteGroupMessage(groupId, messageId, user.getId());
            socketIOService.sendToUser(user.getId(), "group_delete_message", response);
            if (Boolean.TRUE.equals(response.get("status"))) {
                publishToParticipants(groupService.getParticipantIds(groupId), "group_message_deleted", Map.of(
                        "group_id", groupId,
                        "message_id", messageId,
                        "user_id", user.getId()
                ));
            }
        });
    }

    private void sendDirectMessage(SocketIOClient client, Map<String, Object> payload, boolean media) throws Exception {
        User user = user(client);
        String conversationId = required(payload, "conversation_id");
        String to = required(payload, "to");
        Message message = new Message()
                .setFrom(user.getId())
                .setTo(to)
                .setText(optional(payload, "message"))
                .setType(MessageType.valueOf(required(payload, "type")));
        if (media) {
            message.setMedia(required(payload, "media")).setMediaType(required(payload, "mediaType"));
        }
        messageService.addMessage(conversationId, message);
        Map<String, Object> event = Map.of("conversation_id", conversationId, "message", message);
        socketIOService.sendToUser(user.getId(), "new_message", event);
        socketIOService.sendToUser(to, "new_message", event);
    }

    private void sendGroupMessage(SocketIOClient client, Map<String, Object> payload, boolean media) {
        User user = user(client);
        String groupId = required(payload, "group_id");
        Message message = new Message()
                .setFrom(user.getId())
                .setTo(groupId)
                .setText(optional(payload, "message"))
                .setType(MessageType.valueOf(required(payload, "type")));
        if (media) {
            message.setMedia(required(payload, "media")).setMediaType(required(payload, "mediaType"));
        }
        Map<String, Object> response = groupService.addGroupMessage(groupId, message);
        if (Boolean.TRUE.equals(response.get("status"))) {
            publishToParticipants(groupService.getParticipantIds(groupId), "group_new_message", Map.of(
                    "group_id", groupId,
                    "message", message
            ));
        } else {
            socketIOService.sendToUser(user.getId(), "group_new_message", response);
        }
    }

    private void publishToParticipants(List<String> participantIds, String event, Object payload) {
        participantIds.forEach(participantId -> socketIOService.sendToUser(participantId, event, payload));
    }

    private User user(SocketIOClient client) {
        User user = client.get("user");
        if (user == null) {
            throw new IllegalStateException("Unauthenticated socket");
        }
        return user;
    }

    private String required(Map<String, Object> payload, String field) {
        String value = optional(payload, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String optional(Map<String, Object> payload, String field) {
        return payload.get(field) == null ? null : payload.get(field).toString();
    }
}
