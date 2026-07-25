package com.fren507.webchat.managers;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class TabListManager {

    public void updateHeader(MinecraftServer server) {

        Component header = Component.literal(
                "§b§lWEB CHAT\n" +
                        "§7────────────\n" +
                        "§fOnline Web-Spieler"
        );

        Component footer = Component.literal(
                "§7────────────\n" +
                        "§aWebchat aktiv"
        );


        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            player.connection.send(
                    new ClientboundTabListPacket(
                            header,
                            footer
                    )
            );

        }
    }
}