package com.connecto.services;

import com.connecto.model.Message;

import java.util.List;
import java.util.Map;

public interface GroupService {

    Map<String, Object> getAllGroups(String userId);

    Map<String, Object> createGroup(String ownerId, String groupName, String groupAvatar, List<String> memberIds);

    Map<String, Object> getGroupMessages(String userId, String groupId, String lastVisible, Integer limit);

    Map<String, Object> updateGroup(String userId, String groupId, String groupName, String groupAvatar);

    Map<String, Object> addMembers(String userId, String groupId, List<String> memberIds);

    Map<String, Object> removeMembers(String userId, String groupId, List<String> memberIds);

    Map<String, Object> transferOwnership(String userId, String groupId, String newOwnerId);

    Map<String, Object> leaveGroup(String userId, String groupId);

    Map<String, Object> deleteGroup(String userId, String groupId);

    Map<String, Object> addGroupMessage(String groupId, Message message);

    void resetUnreadCount(String userId, String groupId);

    Map<String, Object> deleteGroupMessage(String groupId, String messageId, String userId);

    List<String> getParticipantIds(String groupId);
}
