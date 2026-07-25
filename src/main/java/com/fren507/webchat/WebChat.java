package com.fren507.webchat;

import com.fren507.webchat.managers.VerifiedProfileManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class WebChat implements ModInitializer {
    public static final String MOD_ID = "web-chat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");

        VerifiedProfileManager manager = new VerifiedProfileManager();
        WebServer api = new WebServer(8080, manager, LOGGER);
        WebSocketServer socket = new WebSocketServer(9092, manager, LOGGER, URI.create("http://100.109.207.66:5173/"));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                socket.start(server);
            } catch (Exception e) {
                LOGGER.error("Failed to start WebSocket", e);
            }
        });

        try {
            api.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start API", e);
        }
    }
}

