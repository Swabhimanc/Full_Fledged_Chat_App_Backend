package com.connecto.controller;

import com.connecto.model.Message;
import com.connecto.services.MessageService;
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

    @PostMapping("/addmsg")
    public ResponseEntity<?> addMessage(@RequestBody Message message) {
        try {
            Object result = messageService.addMessage(message);
            return ResponseEntity.status(200).body(result);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/getmsg")
    public ResponseEntity<?> getAllMessages(@RequestParam String from, @RequestParam String to) {
        try {
            List<Map<?,?>> messages = messageService.getAllMessages(from, to);
            return ResponseEntity.status(200).body(new HashMap<>() {{
                put("status", true);
                put("data", messages.toArray());
            }});
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    @PostMapping("/getLimitedMessage")
    public ResponseEntity<?> getLimitedMessage(@RequestBody Object object) {
        try {
            Map request = (Map)object;
            Object response = messageService.getLimitedMessage((String)request.get("from"),(String)request.get("to"),(String)request.get("lastVisible"),(Integer)request.get("limit"));
            return ResponseEntity.status(200).body(response);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
