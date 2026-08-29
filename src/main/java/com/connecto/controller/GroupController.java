package com.connecto.controller;

import com.connecto.model.User;
import com.connecto.model.Group;
import com.connecto.services.GroupService;
import com.connecto.socketIO.SocketIOService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/group")
public class GroupController {
    @Autowired
    GroupService groupService;
    @Autowired
    SocketIOService socketIOService;

    @GetMapping("/get-all-groups")
    public ResponseEntity<?> getAllGroups(HttpServletRequest request) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            Map<String, Object> response = groupService.getAllGroups(fromUser.getId());
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(200).body(e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            String groupName = payload.get("groupName") == null ? null : payload.get("groupName").toString();
            String groupAvatar = payload.get("groupAvatar") == null ? null : payload.get("groupAvatar").toString();
            List<String> members = payload.get("members") instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : new ArrayList<>();

            Map<String, Object> response = groupService.createGroup(fromUser.getId(), groupName, groupAvatar, members);
            publishGroupUpdate(response);
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<?> getGroupMessages(
            @PathVariable String groupId,
            HttpServletRequest request,
            @RequestParam(required = false) String lastVisible,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            Map<String, Object> response = groupService.getGroupMessages(fromUser.getId(), groupId, lastVisible, limit);
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            String groupName = payload.get("groupName") == null ? null : payload.get("groupName").toString();
            String groupAvatar = payload.get("groupAvatar") == null ? null : payload.get("groupAvatar").toString();
            Map<String, Object> response = groupService.updateGroup(fromUser.getId(), groupId, groupName, groupAvatar);
            publishGroupUpdate(response);
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members/add")
    public ResponseEntity<?> addMembers(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            List<String> members = payload.get("members") instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : new ArrayList<>();
            Map<String, Object> response = groupService.addMembers(fromUser.getId(), groupId, members);
            publishGroupUpdate(response);
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members/remove")
    public ResponseEntity<?> removeMembers(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            List<String> members = payload.get("members") instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : new ArrayList<>();
            List<String> previousParticipants = groupService.getParticipantIds(groupId);
            Map<String, Object> response = groupService.removeMembers(fromUser.getId(), groupId, members);
            publishGroupUpdate(response);
            if ((boolean) response.get("status")) {
                previousParticipants.stream()
                        .filter(members::contains)
                        .forEach(id -> socketIOService.sendToUser(id, "group_removed", Map.of("group_id", groupId)));
            }
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/transfer-ownership")
    public ResponseEntity<?> transferOwnership(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            String newOwnerId = payload.get("newOwnerId") == null ? null : payload.get("newOwnerId").toString();
            Map<String, Object> response = groupService.transferOwnership(fromUser.getId(), groupId, newOwnerId);
            publishGroupUpdate(response);
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable String groupId, HttpServletRequest request) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            Map<String, Object> response = groupService.leaveGroup(fromUser.getId(), groupId);
            publishGroupUpdate(response);
            if ((boolean) response.get("status")) {
                socketIOService.sendToUser(fromUser.getId(), "group_removed", Map.of("group_id", groupId));
            }
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/delete-message")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            String messageId = payload.get("messageId") == null ? null : payload.get("messageId").toString();
            Map<String, Object> response = groupService.deleteGroupMessage(groupId, messageId, fromUser.getId());
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable String groupId, HttpServletRequest request) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            List<String> participants = groupService.getParticipantIds(groupId);
            Map<String, Object> response = groupService.deleteGroup(fromUser.getId(), groupId);
            if ((boolean) response.get("status")) {
                participants.forEach(id -> socketIOService.sendToUser(
                        id, "group_removed", Map.of("group_id", groupId)
                ));
            }
            return ResponseEntity.status((boolean) response.get("status") ? 200 : 400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    private void publishGroupUpdate(Map<String, Object> response) {
        if (!(boolean) response.getOrDefault("status", false) || !(response.get("data") instanceof Group group)) {
            return;
        }
        group.getParticipants().forEach(participant ->
                socketIOService.sendToUser(participant.getId(), "group_updated", group)
        );
    }
}
