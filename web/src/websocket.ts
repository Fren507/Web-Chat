import type {VerificationProfile} from "./verificationCode.ts";
import {io, Socket} from "socket.io-client";
import {minecraftToHTML} from "./minecraftToHTML.ts";
import {getMessageNode} from "./minecraftSkin.ts";
import {serverUrl} from "./main.ts";

export function createMessage(
    message: string,
    sessionID: string,
): {
    sessionID: string;
    message: string;
} {
    return {sessionID: sessionID, message: message};
}

export function connectWebSocket(): {
    socket?: Socket;
    profile?: VerificationProfile;
    sessionID?: string;
    valid: boolean;
} {
    const status = document.querySelector("#connection-status");
    const chatBox = document.querySelector("#chat-box");
    const messageInput =
        document.querySelector<HTMLInputElement>("#message-input");
    const noMessagesCard = document.getElementById("no-messages-card");
    const sendBtn = document.querySelector<HTMLButtonElement>("#send-btn");

    const rawSessionProfile = sessionStorage.getItem("web-chat-profile");
    const rawSessionID = sessionStorage.getItem("web-chat-session");
    if (!rawSessionProfile || !rawSessionID) {
        console.error(
            "No session profile or ID stored: ",
            rawSessionID,
            rawSessionProfile,
        );
        return {valid: false};
    }
    const profile = JSON.parse(rawSessionProfile) as VerificationProfile;
    if (profile.sessionID != rawSessionID) {
        console.error("Stored sessionID is not the same as session profile!");
        return {valid: false};
    }
    const sessionID = rawSessionID;

    // getPlayerSkin(profile.playerUUID)

    //const socket = io(socketUrl);
    //const socket = io("http://localhost:9124", {
    //    transports: ["websocket"],
    //});
    const socket = io(serverUrl);

    socket.on("connect", () => {
        console.log("Connected to Java Server!");

        socket.emit(
            "chatMessage",
            createMessage(sessionID, `Hallo von ${profile.username}!`),
        );
        if (status) status.textContent = "Verbunden";

        // Enable inputs now that we're connected!
        if (messageInput) messageInput.disabled = false;
        if (sendBtn) sendBtn.disabled = false;
    });

    // Listen for broadcasted messages
    socket.on(
        "newMessage",
        (data: {
            username: string;
            message: string;
            senderUUID: string;
            fromWeb: boolean;
        }) => {
            if (chatBox) {
                getMessageNode(
                    data.username,
                    minecraftToHTML(
                        `§r <${data.username}${data.fromWeb ? " §b[WEB]" : ""}§r> ${data.message}`,
                    ),
                    [data.senderUUID],
                    serverUrl.toString(),
                ).then((message) => {
                    chatBox.appendChild(message);
                });

                chatBox.scrollTop = chatBox.scrollHeight; // Auto-scroll to bottom
            }
            if (noMessagesCard) noMessagesCard.remove();
        },
    );

    // {
    //   "messageType": "PlayerLeave",
    //   "message": "ShadowBits",
    //   "affectedPlayers": [
    //     "5ccc7940-3389-4c42-9e53-3f6e129e6e56"
    //   ]
    // }

    type MessageType = "PlayerLeave" | "PlayerJoin" | "GameMessage";

    socket.on(
        "serverMessage",
        (data: {
            messageType: MessageType;
            message: string;
            affectedPlayers: string[];
        }) => {
            if (chatBox) {
                switch (data.messageType) {
                    case "PlayerJoin":
                        getMessageNode(
                            data.message,
                            minecraftToHTML(
                                `§e${data.message} hat das Spiel betreten`,
                            ),
                            data.affectedPlayers,
                            serverUrl.toString(),
                        ).then((message) => {
                            chatBox.appendChild(message);
                        });
                        break;

                    case "PlayerLeave":
                        getMessageNode(
                            data.message,
                            minecraftToHTML(
                                `§e${data.message} hat das Spiel verlassen`,
                            ),
                            data.affectedPlayers,
                            serverUrl.toString(),
                        ).then((message) => {
                            chatBox.appendChild(message);
                        });
                        break;
                    case "GameMessage":
                        getMessageNode(
                            data.message,
                            minecraftToHTML(
                                `§e${data.message} hat das Spiel verlassen`,
                            ),
                            data.affectedPlayers,
                            serverUrl.toString(),
                        ).then((message) => {
                            chatBox.appendChild(message);
                        });
                        break;
                    default:
                        return;
                }

                chatBox.scrollTop = chatBox.scrollHeight; // Auto-scroll to bottom
            }
        },
    );

    socket.on("close", () => {
        if (status) status.textContent = "Getrennt";
        if (messageInput) messageInput.disabled = true;
        if (sendBtn) sendBtn.disabled = true;
    });

    return {
        socket,
        valid: true,
        profile,
        sessionID,
    };
}

export function handleWebSocket(): {
    profile: VerificationProfile;
    sessionID: string;
} | null {
    const connectWebSocketReturn = connectWebSocket();
    if (
        !connectWebSocketReturn.valid ||
        !connectWebSocketReturn.socket ||
        !connectWebSocketReturn.profile ||
        !connectWebSocketReturn.sessionID
    ) {
        const dialog = document.getElementById("login") as HTMLDialogElement;
        dialog.showModal();
        return null;
    } else {
        const messageInput =
            document.querySelector<HTMLInputElement>("#message-input")!;
        const sendBtn = document.querySelector<HTMLButtonElement>("#send-btn")!;

        const socket = connectWebSocketReturn.socket;
        const sessionID = connectWebSocketReturn.sessionID;
        const profile = connectWebSocketReturn.profile;

        console.log(profile);

        function sendMessage() {
            const text = messageInput.value.trim();
            if (text !== "" && socket.connected) {
                socket.emit("chatMessage", createMessage(text, sessionID));
                messageInput.value = "";
            }
        }

        sendBtn.addEventListener("click", sendMessage);
        messageInput.addEventListener(
            "keydown",
            (e) => e.key === "Enter" && sendMessage(),
        );

        return {
            profile,
            sessionID,
        };
    }
}
