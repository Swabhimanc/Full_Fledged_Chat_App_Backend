package com.connecto.controller;

import com.connecto.model.Message;
import com.connecto.model.User;
import com.connecto.services.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/getmsg")
    public ResponseEntity<?> getAllMessages(@RequestParam String to, HttpServletRequest request) {
        try {
            User user = requireUser(request);
            List<Map<?,?>> messages = messageService.getAllMessages(user.getId(), to);
            return ResponseEntity.status(200).body(new HashMap<>() {{
                put("status", true);
                put("data", messages.toArray());
            }});
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/getLimitedMessage")
    public ResponseEntity<?> getLimitedMessage(@RequestBody Object object, HttpServletRequest httpRequest) {
        try {
            User user = requireUser(httpRequest);
            Map request = (Map)object;
            Object response = messageService.getLimitedMessage(user.getId(), (String)request.get("to"),(String)request.get("lastVisible"),(Integer)request.get("limit"));
            return ResponseEntity.status(200).body(response);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/get_one_to_one")
    public ResponseEntity<?> getOneToOne(HttpServletRequest request) throws ExecutionException, InterruptedException {
        try{
            Map<String,Object> response = messageService.allDirectConversations(requireUser(request).getId());
            return ResponseEntity.status(200).body(response);
        }catch (Exception e){
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/start_conversation")
    public ResponseEntity<?> startConversation(@RequestBody Map<String,Object>payload, HttpServletRequest request) throws ExecutionException, InterruptedException {
        try{
            Map<String,Object> response = messageService.startConversation(requireUser(request).getId(), payload.get("to").toString());
            return ResponseEntity.status(200).body(response);
        }catch (Exception e){
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    private User requireUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("user");
        if (user == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        return user;
    }
}
