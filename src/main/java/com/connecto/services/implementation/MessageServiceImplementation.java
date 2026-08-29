package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.model.Message;
import com.connecto.model.OneToOneMessage;
import com.connecto.repositories.MessageRepository;
import com.connecto.repositories.OneToOneMessageRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.services.MessageService;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class MessageServiceImplementation implements MessageService {
    private static final int CONVERSATION_PREVIEW_LIMIT = 50;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OneToOneMessageRepository oneToOneMessageRepository;

    public MessageServiceImplementation(
            MessageRepository messageRepository,
            UserRepository userRepository,
            OneToOneMessageRepository oneToOneMessageRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.oneToOneMessageRepository = oneToOneMessageRepository;
    }

    @Override
    public void addMessage(String conversationId, Message message) throws ExecutionException, InterruptedException {
        requireParticipant(conversationId, message.getFrom());
        requireParticipant(conversationId, message.getTo());
        try {
            oneToOneMessageRepository.addMessageToConversation(conversationId, message);
        } catch (ExecutionException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save message", e);
        }
    }

    @Override
    public List<Map<?, ?>> getAllMessages(String from, String to) throws ExecutionException, InterruptedException {
        List<Map<?, ?>> messages = new ArrayList<>();
        QuerySnapshot querySnapshot = messageRepository.getAllFromAndToMessages(from, to);
        SimpleDateFormat dateFormat = dateFormat();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            messages.add(Map.of(
                    "fromSelf", from.equals(doc.get("from")),
                    "createdAt", dateFormat.format(doc.getTimestamp("createdAt").toDate()),
                    "message", Objects.toString(doc.get("message"), ""),
                    "type", Objects.toString(doc.get("type"), "")
            ));
        }
        return messages;
    }

    @Override
    public Object getLimitedMessage(String fromUser, String toUser, String lastVisible, Integer limit) throws ExecutionException, InterruptedException {
        int pageSize = limit == null ? 50 : Math.max(1, Math.min(limit, 100));
        Query query = messageRepository.getMessageRef()
                .whereIn("from", List.of(fromUser, toUser))
                .whereIn("to", List.of(fromUser, toUser))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize);
        if (lastVisible != null && !lastVisible.isBlank()) {
            query = query.startAfter(messageRepository.getMessageRef().document(lastVisible).get().get());
        }

        List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();
        List<Map<?, ?>> messages = new ArrayList<>();
        SimpleDateFormat dateFormat = dateFormat();
        for (DocumentSnapshot doc : documents) {
            messages.add(Map.of(
                    "fromSelf", fromUser.equals(doc.get("from")),
                    "createdAt", dateFormat.format(Objects.requireNonNull(doc.getTimestamp("createdAt")).toDate()),
                    "message", Objects.toString(doc.get("message"), ""),
                    "type", Objects.toString(doc.get("type"), "")
            ));
        }
        Collections.reverse(messages);
        String cursor = documents.isEmpty() ? null : documents.get(documents.size() - 1).getId();
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", messages);
        response.put("lastVisible", cursor);
        return response;
    }

    @Override
    public Map<String, Object> allDirectConversations(String userId) throws ExecutionException, InterruptedException {
        DocumentReference userRef = userRepository.findUserReferenceById(userId);
        List<QueryDocumentSnapshot> conversations = oneToOneMessageRepository.getAllDirectConversations(userRef);

        Set<String> participantIds = new HashSet<>();
        for (QueryDocumentSnapshot conversation : conversations) {
            for (DocumentReference participant : participantRefs(conversation)) {
                participantIds.add(participant.getId());
            }
        }
        Map<String, UserResponseDTO> users = usersById(participantIds);

        List<OneToOneMessage> response = new ArrayList<>();
        for (QueryDocumentSnapshot conversation : conversations) {
            try {
                oneToOneMessageRepository.ensureMigrated(conversation);
                response.add(mapConversation(conversation, userId, users));
            } catch (ExecutionException | InterruptedException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to load conversation", e);
            }
        }

        return Map.of(
                "status", true,
                "message", "Direct Conversations Fetched Successfully",
                "data", response
        );
    }

    @Override
    public Map<String, Object> startConversation(String from, String to) throws ExecutionException, InterruptedException {
        if (from.equals(to)) {
            throw new IllegalArgumentException("Cannot start a conversation with yourself");
        }
        DocumentSnapshot recipient = userRepository.findUserById(to);
        if (!recipient.exists()) {
            throw new IllegalArgumentException("User not found");
        }

        try {
            DocumentSnapshot conversation = oneToOneMessageRepository.getOrCreateDirectConversation(
                    userRepository.findUserReferenceById(from),
                    userRepository.findUserReferenceById(to)
            );
            oneToOneMessageRepository.ensureMigrated(conversation);
            Map<String, UserResponseDTO> users = usersById(Set.of(from, to));
            OneToOneMessage data = mapConversation(conversation, from, users);
            return Map.of(
                    "status", true,
                    "message", "Conversation ready",
                    "data", data
            );
        } catch (ExecutionException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to start conversation", e);
        }
    }

    @Override
    public List<Message> getOneToOneMessages(String id, String userId) throws ExecutionException, InterruptedException {
        requireParticipant(id, userId);
        try {
            return oneToOneMessageRepository.getMessages(id, 100, userId);
        } catch (ExecutionException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load messages", e);
        }
    }

    @Override
    public void resetUnreadCount(String userId, String conversationId) throws ExecutionException, InterruptedException {
        requireParticipant(conversationId, userId);
        oneToOneMessageRepository.resetUnreadCount(conversationId, userId);
    }

    @Override
    public CompletableFuture<HashMap<String, Object>> deleteMessage(String conversationId, String messageId, String userId) throws ExecutionException, InterruptedException {
        requireParticipant(conversationId, userId);
        try {
            oneToOneMessageRepository.deleteMessageForUser(conversationId, messageId, userId);
            return CompletableFuture.completedFuture(success("Message deleted successfully"));
        } catch (ExecutionException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return CompletableFuture.completedFuture(failure(e.getMessage()));
        }
    }

    @Override
    public CompletableFuture<HashMap<String, Object>> deleteChat(String roomId, String userId) throws ExecutionException, InterruptedException {
        requireParticipant(roomId, userId);
        oneToOneMessageRepository.hideChat(roomId, userId);
        return CompletableFuture.completedFuture(success("Chat deleted successfully"));
    }

    private OneToOneMessage mapConversation(
            DocumentSnapshot conversation,
            String userId,
            Map<String, UserResponseDTO> users
    ) throws Exception {
        List<UserResponseDTO> participants = participantRefs(conversation).stream()
                .map(DocumentReference::getId)
                .map(users::get)
                .filter(Objects::nonNull)
                .toList();
        Map<String, Long> unreadCounts = normalizeCounts(conversation.get("unreadCounts"));
        List<Message> messages = oneToOneMessageRepository.getMessages(conversation.getId(), CONVERSATION_PREVIEW_LIMIT, userId);

        return new OneToOneMessage()
                .setId(conversation.getId())
                .setParticipants(participants)
                .setMessages(messages)
                .setUnreadCounts(unreadCounts)
                .setCreatedAt(conversation.getTimestamp("createdAt") == null
                        ? null
                        : conversation.getTimestamp("createdAt").toDate());
    }

    private Map<String, UserResponseDTO> usersById(Set<String> userIds) throws ExecutionException, InterruptedException {
        Map<String, UserResponseDTO> users = new HashMap<>();
        for (DocumentSnapshot user : userRepository.findUsersByIds(new ArrayList<>(userIds))) {
            if (user.exists()) {
                users.put(user.getId(), user.toObject(UserResponseDTO.class));
            }
        }
        return users;
    }

    private List<DocumentReference> participantRefs(DocumentSnapshot conversation) {
        List<DocumentReference> participants = (List<DocumentReference>) conversation.get("participants");
        return participants == null ? List.of() : participants;
    }

    private void requireParticipant(String conversationId, String userId) throws ExecutionException, InterruptedException {
        DocumentSnapshot conversation = oneToOneMessageRepository.getConversationById(conversationId).get().get();
        if (!conversation.exists()) {
            throw new IllegalArgumentException("Conversation not found");
        }
        boolean allowed = participantRefs(conversation).stream().anyMatch(ref -> ref.getId().equals(userId));
        if (!allowed) {
            throw new AccessDeniedException("Not a conversation participant");
        }
    }

    private Map<String, Long> normalizeCounts(Object value) {
        Map<String, Long> counts = new HashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, count) -> {
                if (key != null && count instanceof Number number) {
                    counts.put(key.toString(), number.longValue());
                }
            });
        }
        return counts;
    }

    private SimpleDateFormat dateFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        return format;
    }

    private HashMap<String, Object> success(String message) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("message", message);
        return response;
    }

    private HashMap<String, Object> failure(String message) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("status", false);
        response.put("message", message);
        return response;
    }
}
