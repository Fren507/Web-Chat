package com.fren507.webchat;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.fren507.webchat.models.ChatMessage;

public class WebSocketServer {

    private final int port;
    private SocketIOServer server;

    public WebSocketServer(int port) {
        this.port = port;
    }

    // Changed from "public static void start()" to "public void start()"
    public void start() {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(port);

        server = new SocketIOServer(config);

        // 3. Connection Listener
        server.addConnectListener(client -> {
            System.out.println("Client connected: " + client.getSessionId());

            // Send a welcome event back to this specific client
            client.sendEvent("welcome", "Connected to Netty-SocketIO server!");
        });

        // 4. Disconnection Listener
        server.addDisconnectListener(client -> {
            System.out.println("Client disconnected: " + client.getSessionId());
        });

        // 5. Custom Event Listener: "chatMessage"
        server.addEventListener("chatMessage", ChatMessage.class, (client, data, ackSender) -> {
            System.out.println(data.getUsername() + ": " + data.getMessage());

            // Broadcast the message to ALL connected clients
            server.getBroadcastOperations().sendEvent("newMessage", data);
        });

        // 6. Start the server!
        server.start();
        System.out.println("🚀 Socket.IO Server started on port 9092!");

        // Prevent the main thread from exiting instantly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping Socket.IO server...");
            server.stop();
        }));
    }
}