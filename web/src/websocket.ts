import type {VerificationProfile} from "./verificationCode.ts";
import {io, Socket} from "socket.io-client";
import {minecraftToHTML} from "./minecraftToHTML.ts";

const SOCKET_URL = import.meta.env.VITE_SOCKET_URL;

export function createMessage(message: string, sessionID: string): {
    sessionID: string,
    message: string
} {
    return {sessionID: sessionID, message: message}
}

export function connectWebSocket(): {
    socket?: Socket,
    profile?: VerificationProfile,
    sessionID?: string,
    valid: boolean
} {
    const status = document.querySelector("#status");
    const title = document.querySelector("#title");
    const chatBox = document.querySelector("#chat-box");
    const messageInput =
        document.querySelector<HTMLInputElement>("#message-input");
    const sendBtn = document.querySelector<HTMLButtonElement>("#send-btn");

    const rawSessionProfile = sessionStorage.getItem("web-chat-profile");
    const rawSessionID = sessionStorage.getItem("web-chat-session");
    if (!rawSessionProfile || !rawSessionID) {
        console.error("No session profile or ID stored: ", rawSessionID, rawSessionProfile);
        return {valid: false};
    }
    const profile = JSON.parse(rawSessionProfile) as VerificationProfile;
    if (profile.sessionID != rawSessionID) {
        console.error("Stored sessionID is not the same as session profile!")
        return {valid: false};
    }
    const sessionID = rawSessionID;

    // getPlayerSkin(profile.playerUUID)

    const socket = io(SOCKET_URL)

    socket.on("connect", () => {
        console.log("Connected to Java Server!");

        socket.emit("chatMessage", createMessage(sessionID, `Hallo von ${profile.username}!`));
        if (status) status.textContent = "Verbunden";
        if (title) title.textContent = "WebChat";

        // Enable inputs now that we're connected!
        if (messageInput) messageInput.disabled = false;
        if (sendBtn) sendBtn.disabled = false;
    });

    // Listen for broadcasted messages
    socket.on("newMessage", (data: {
        username: string,
        message: string
    }) => {
        if (chatBox) {
            const msgElement = document.createElement("div");
            msgElement.innerHTML = minecraftToHTML(`§r <${data.username}§r> ${data.message}`);
            chatBox.appendChild(msgElement);
            chatBox.scrollTop = chatBox.scrollHeight; // Auto-scroll to bottom
        }
    });

    socket.on("close", () => {
        if (status) status.textContent = "Getrennt";
        if (messageInput) messageInput.disabled = true;
        if (sendBtn) sendBtn.disabled = true;
    });

    return {
        socket,
        valid: true,
        profile,
        sessionID
    };
}
