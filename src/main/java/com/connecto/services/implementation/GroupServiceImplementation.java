package com.connecto.services.implementation;

import com.connecto.services.GroupService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GroupServiceImplementation implements GroupService {
    @Override
    public Map<String, Object> getAllGroups(String id) {
        return Map.of();
    }
}
