package com.fren507.webchat.website_backend;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.fren507.webchat.helpers.ChatUtil;
import com.fren507.webchat.managers.TabListManager;
import com.fren507.webchat.managers.VerifiedProfileManager;
import com.fren507.webchat.models.ChatMessage;
import com.fren507.webchat.models.ChatMessageData;
import com.fren507.webchat.models.ServerMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class WebSocketServer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final int port;
    private final VerifiedProfileManager profileManager;
    private final Logger LOGGER;
    private final URI websiteURI;
    private SocketIOServer server;
    private boolean isRunning;

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
        isRunning = true;

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
                        ChatMessage chatMessage = new ChatMessage(profile.getUsername(), message, profile.getPlayerUUID(), true);
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

    public void sendMessageToWeb(String message, String senderUsername, UUID senderUUID) {
        if (server == null) return;
        ChatMessage chatMessage = new ChatMessage(senderUsername, message, senderUUID, false);
        server.getBroadcastOperations().sendEvent("newMessage", chatMessage);
    }

    public void sendServerMessageToWeb(String messageType, @Nullable String message, List<UUID> affectedPlayers) {
        if (server == null) return;
        ServerMessage serverMessage = new ServerMessage(messageType, message, affectedPlayers);
        server.getBroadcastOperations().sendEvent("serverMessage", serverMessage);
    }

    public void stop() {
        server.stop();
        isRunning = false;
    }

    public boolean isStopped() {
        return !isRunning;
    }
}