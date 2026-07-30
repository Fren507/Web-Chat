package com.fren507.webchat.models;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.UUID;

public class ChatMessage {
    private String username;
    private String message;
    private boolean fromWeb;
    private UUID senderUUID;

    // Default constructor is required for Jackson JSON parsing!
    public ChatMessage() {
    }

    public ChatMessage(String username, String message, UUID senderUUID, boolean fromWeb) {
        this.username = username;
        this.message = message;
        this.senderUUID = senderUUID;
        this.fromWeb = fromWeb;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isFromWeb() {
        return fromWeb;
    }

    public void setFromWeb(boolean fromWeb) {
        this.fromWeb = fromWeb;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public void setSenderUUID(UUID senderUUID) {
        this.senderUUID = senderUUID;
    }

    public Component getMessageComponent() {
        return Component.literal(message);
    }

    public Component getUsernamePrefix(@Nullable URI websiteURL) {
        URI url = websiteURL != null
                ? websiteURL
                : URI.create("https://github.com/Fren507/Web-Chat/");

        String hoverText = websiteURL != null
                ? "Visit Web Chat!"
                : "For further information visit the mod on GitHub!";

        return Component
                .literal("<" + username + " ")
                .append(Component.literal("[WEB]")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(
                                        new HoverEvent.ShowText(
                                                Component.literal(hoverText)
                                        )
                                )
                                .withClickEvent(
                                        new ClickEvent.OpenUrl(url)
                                )
                        ))
                .append(Component.literal(">"));
    }

    public Component getChatComponent(@Nullable URI websiteURL) {
        return getUsernamePrefix(websiteURL).copy()
                .append(Component.literal(" "))
                .append(getMessageComponent());
    }
}
