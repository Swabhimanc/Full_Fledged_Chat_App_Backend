package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.model.Message;
import com.connecto.model.OneToOneMessage;
import com.connecto.repositories.MessageRepository;
import com.connecto.repositories.OneToOneMessageRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.services.MessageService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class MessageServiceImplementation implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OneToOneMessageRepository oneToOneMessageRepository;

    @Autowired
    public MessageServiceImplementation(MessageRepository messageRepository, UserRepository userRepository, OneToOneMessageRepository oneToOneMessageRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.oneToOneMessageRepository = oneToOneMessageRepository;
    }

    public void addMessage(String conversation_id, Message message) throws ExecutionException, InterruptedException {
        oneToOneMessageRepository.addMessageToConversation(conversation_id,message);
    }

    public List<Map<?, ?>> getAllMessages(String from, String to) throws ExecutionException, InterruptedException {
        List<Map<?, ?>> messages = new ArrayList<>();

        QuerySnapshot querySnapshot = messageRepository.getAllFromAndToMessages(from, to);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

            messages.add(new HashMap<>() {{
                put("fromSelf", from.equals(doc.get("from")));
                put("createdAt", dateFormat.format(doc.getTimestamp("createdAt").toDate()));
                put("message", doc.get("message"));
                put("type", doc.get("type"));
            }});
        }
        return messages;
    }

    public Object getLimitedMessage(String fromUser, String toUser, String lastVisible, Integer limit) throws ExecutionException, InterruptedException {
        List<Map<?, ?>> messages = new ArrayList<>();

        // Base query to get messages between two users, ordered by creation time
        Query query = messageRepository.getMessageRef().whereIn("from", List.of(fromUser, toUser)).whereIn("to", List.of(fromUser, toUser)).orderBy("createdAt", Query.Direction.DESCENDING).limit(limit);

        // Handle offset by skipping the required number of documents
        if (lastVisible != null) {
            query = query.startAfter(messageRepository.getMessageRef().document(lastVisible).get().get());
        }

        // Fetch the required batch of messages
        List<QueryDocumentSnapshot> querySnapshot = query.get().get().getDocuments();
        DocumentSnapshot last = !querySnapshot.isEmpty() ? querySnapshot.get(querySnapshot.size() - 1) : null;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        for (DocumentSnapshot doc : querySnapshot) {
            messages.add(new HashMap<>() {{
                put("fromSelf", fromUser.equals(doc.get("from")));
                put("createdAt", dateFormat.format(Objects.requireNonNull(doc.getTimestamp("createdAt")).toDate()));
                put("message", doc.get("message"));
                put("type", doc.get("type"));
            }});
        }
        Collections.reverse(messages);
        return new HashMap<>() {{
            put("status", true);
            put("data", messages);
            put("lastVisible", last == null ? last : last.getId());
        }};
    }

    @Override
    public Map<String, Object> allDirectConversations(String userId) throws ExecutionException, InterruptedException {
//        Map<String, Object> req = (Map<String, Object>) object;
        DocumentReference userRef = userRepository.findUserReferenceById(userId);
        List<QueryDocumentSnapshot> result = oneToOneMessageRepository.getAllDirectConversations(userRef);
        List<OneToOneMessage> response = new ArrayList<>();

        result.forEach(doc -> {
            List<UserResponseDTO> participants = new ArrayList<>();
            ((List<DocumentReference>) doc.get("participants")).forEach(el -> {
                if (!el.getId().equals(userRef.getId())) {
                    try {
                        participants.add(el.get().get().toObject(UserResponseDTO.class));
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            List<Message> messages = (List<Message>) doc.get("messages");
            response.add(new OneToOneMessage().setMessages(messages).setId(doc.getId()).setParticipants(participants));
        });
        return new HashMap<>() {{
            put("status", true);
            put("data", response);
        }};
    }

    public Map<String, Object> startConversation(String from, String to) throws ExecutionException, InterruptedException {
        DocumentReference fromRef = userRepository.findUserReferenceById(from);
        DocumentReference toRef = userRepository.findUserReferenceById(to);
        boolean chatExists = true;

        OneToOneMessage oneToOneMessage = new OneToOneMessage();
        List<QueryDocumentSnapshot> result = oneToOneMessageRepository.getDirectConversation(fromRef, toRef);

        if (result.isEmpty()) {
            WriteResult writeResult = oneToOneMessageRepository.createDirectConversation(fromRef, toRef);
            chatExists = false;
            if (writeResult==null) {
                return new HashMap<>() {{
                    put("status", false);
                    put("message", "Failed to fetch Conversation");
                }};
            }
            result = oneToOneMessageRepository.getDirectConversation(fromRef, toRef);
        }
        result.forEach(doc -> {
            List<UserResponseDTO> participants = new ArrayList<>();
            ((List<DocumentReference>) doc.get("participants")).forEach(el -> {
                try {
                    participants.add(el.get().get().toObject(UserResponseDTO.class));
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
            List<Message> messages = (List<Message>) doc.get("messages");
            oneToOneMessage.setMessages(messages)
                    .setId(doc.getId())
                    .setParticipants(participants);
        });

        final boolean finalChatExists = chatExists;
        return new HashMap<>() {{
            put("status", true);
            put("message", finalChatExists ?"Conversation fetched successfully":"New chat created successfully");
            put("data", oneToOneMessage);
        }};
    }

    @Override
    public List<Message> getOneToOneMessages(String id) throws ExecutionException, InterruptedException {
        DocumentReference messageRef = oneToOneMessageRepository.getConversationById(id);
        List<Message> messages = (List<Message>) messageRef.get().get().get("messages");
        if(messages!=null){
            return messages;
        }
        return new ArrayList<>();
    }
}
