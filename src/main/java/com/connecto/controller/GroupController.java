package com.connecto.controller;

import com.connecto.model.User;
import com.connecto.services.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/group")
public class GroupController {
    @Autowired
    GroupService groupService;

    @GetMapping("/get-all-groups")
    public ResponseEntity<?> getAllGroups(HttpServletRequest request) {
        try {
            User fromUser = (User) request.getAttribute("user");
            Map<String, Object> response = groupService.getAllGroups(fromUser.getId());
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(200).body(e.getMessage());
        }
    }
}
