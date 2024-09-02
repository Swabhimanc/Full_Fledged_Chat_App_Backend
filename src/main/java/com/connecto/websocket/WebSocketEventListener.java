package com.connecto.websocket;

import com.connecto.enums.Status;
import com.connecto.model.User;
import com.connecto.services.implementation.UserServiceImplementation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
public class WebSocketEventListener implements ChannelInterceptor {

    @Autowired
    UserServiceImplementation userServiceImplementation;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) throws ExecutionException, InterruptedException {
        try{
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            User user = (User) headerAccessor.getSessionAttributes().get("user");
            userServiceImplementation.setUserStatus(user.getId(), Status.ONLINE);
        }catch (Exception e){

        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try{
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            User user = (User) headerAccessor.getSessionAttributes().get("user");
            userServiceImplementation.setUserStatus(user.getId(), Status.OFFLINE);
        }catch (Exception e){

        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        System.out.println("WebSocket Subscribed: Session ID = " + event);
    }
}

