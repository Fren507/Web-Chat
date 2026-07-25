package com.fren507.webchat;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.fren507.webchat.helpers.ChatUtil;
import com.fren507.webchat.managers.TabListManager;
import com.fren507.webchat.managers.VerifiedProfileManager;
import com.fren507.webchat.models.ChatMessage;
import com.fren507.webchat.models.ChatMessageData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.net.URI;

public class WebSocketServer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final int port;
    private final VerifiedProfileManager profileManager;
    private final Logger LOGGER;
    private final URI websiteURI;
    private SocketIOServer server;

    public WebSocketServer(int port, VerifiedProfileManager profileManager, Logger LOGGER, URI websiteURI) {
        this.port = port;
        this.profileManager = profileManager;
        this.LOGGER = LOGGER;
        this.websiteURI = websiteURI;
    }

    // Changed from "public static void start()" to "public void start()"
    public void start(MinecraftServer minecraftServer) {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(port);

        TabListManager tabListManager = new TabListManager();
        tabListManager.updateHeader(minecraftServer);

        server = new SocketIOServer(config);

        // 3. Connection Listener
        server.addConnectListener(client -> {
            System.out.println("Client connected: " + client.getSessionId());

            // Send a welcome event back to this specific client
            client.sendEvent("welcome", "Connected to Netty-SocketIO server!");
        });

        server.addDisconnectListener(client -> {
            System.out.println("Client disconnected: " + client.getSessionId());
        });

        server.addEventListener("chatMessage", ChatMessageData.class, (client, data, ackSender) -> {
            String sessionID = data.getSessionID();
            String message = data.getMessage();

            profileManager.getProfileViaSession(sessionID)
                    .map((profile) -> {
                        ChatMessage chatMessage = new ChatMessage(profile.getUsername(), message);
                        server.getBroadcastOperations().sendEvent("newMessage", chatMessage);
                        ChatUtil.broadcast(minecraftServer, chatMessage.getChatComponent(websiteURI));
                        return true;
                    });

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