package com.connecto.services.implementation;

import com.connecto.model.Message;
import com.connecto.repositories.MessageRepository;
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

    @Autowired
    public MessageServiceImplementation(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public Object addMessage(Message message) throws ExecutionException, InterruptedException {

        //Validation to check if both users exist
        DocumentSnapshot fromUser = userRepository.findUserById(message.getFrom());
        DocumentSnapshot toUser = userRepository.findUserById(message.getTo());
        if (!fromUser.exists() || !toUser.exists()) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Users not found");
            }};
        }
        //set the createdAt field for the Message
        message.setCreatedAt(new Date());

        //Save the message to the database
        ApiFuture<DocumentReference> result = messageRepository.saveMessage(message);
        return new HashMap<>() {{
            put("status", result.get() != null);
            put("message", "Message sent successfully");
        }};

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
        Query query = messageRepository.getMessageRef().whereIn("from", List.of(fromUser, toUser))
                .whereIn("to", List.of(fromUser, toUser))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

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
}
