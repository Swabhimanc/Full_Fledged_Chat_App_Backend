package com.connecto.socketIO;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class SocketIOService {

    private final SocketIOServer socketIOServer;

    public SocketIOService(SocketIOServer socketIOServer) {
        this.socketIOServer = socketIOServer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startSocketServer() {
        socketIOServer.start();
        System.out.println("Socket.IO server started on port:" + socketIOServer.getConfiguration().getPort());
    }

    @PreDestroy
    public void stopSocketServer() {
        socketIOServer.stop();
        System.out.println("Socket.IO server stopped");
    }

    public void sendToUser(String userId, String event, Object data) {
        if (SocketIOConfig.clientMap.containsKey(userId)) {
            SocketIOConfig.clientMap.get(userId).sendEvent(event, data);
        }
    }
}
