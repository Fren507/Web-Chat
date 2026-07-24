import {setupVerificationInput, verificationLogic} from "./verificationCode.ts";
import "./assets/css/style.css";
import "./assets/css/code.css"
import {connectWebSocket, createMessage} from "./websocket.ts";

const SERVER_URL = import.meta.env.VITE_SERVER_URL;

const verificationCodeHtml = `
<div class="verification-code">
    ${Array.from({length: 3}, (_, group) => `
        <div class="verification-group">
            ${Array.from({length: 4}, (_, index) => `
                <input
                    class="verification-character"
                    type="text"
                    maxlength="1"
                    inputmode="text"
                    autocomplete="off"
                    spellcheck="false"
                    data-index="${group * 4 + index}"
                />
            `).join("")}
        </div>
    `).join('<span class="separator">-</span>')}
</div>
`;

// Render UI layout
document.querySelector<HTMLDivElement>("#app")!.innerHTML = `
  <h1 id="title">MC Auth</h1>
  <p id="status">Verbinde...</p>
  
  <div id="chat-box" style="border: 1px solid #ccc; height: 200px; overflow-y: auto; padding: 10px; margin-bottom: 10px; text-align: left;"></div>

  <div style="display: flex; gap: 5px;">
    <input type="text" id="message-input" placeholder="Nachricht eingeben..." disabled />
    <button id="send-btn" disabled>Senden</button>
  </div>

  <dialog id="login" closedby="none">
    <p>Um diesen Chat nutzten zu können, müssen Sie sich anmelden!</p>
    <details>
        <ol>
            <li>Starten Sie Minecraft und loggen sich auf dem Server ein!</li>
            <li>Nun öffnen Sie den Chat <code><span class="punctuation">(</span><span class="keyword">t</span><span class="punctuation">)</span></code> und geben Sie den Befehl <code><span class="punctuation">/</span><span class="keyword">verify</span></code> ein.</li>
            <li>Nun bekommen Sie einen Code, wie <code><span class="string">XXXX-XXXX-XXXX</span></code>, welcher nach 2 Stunden abläuft!</li>
            <li>Zuletzt müssen Sie nur noch den Code unten eingeben!</li>
        </ol>
        <summary>Wie melde ich mich an?</summary>
    </details>

    <p>Ihr Verifizierungscode:</p>
    ${verificationCodeHtml}
  </dialog>
  
`;

const loginDialog = document.querySelector<HTMLDialogElement>("#login")!;
loginDialog.showModal();

const sessionProfile = sessionStorage.getItem("web-chat-profile");
if (sessionProfile) {
    loginDialog.close();
    connectWebSocket();
}

interface PlayerData {
    id: string,
    name: string,
    properties: {
        name: string,
        value: string
    }[],
    profileActions: unknown[];
}

interface TexturesObject {
    timestamp: number;
    profileId: string;
    profileName: string;
    textures: {
        SKIN?: {
            url: string;
        };
        CAPE?: {
            url: string;
        };
    };
}

export async function getPlayerSkin(playerUUID: string): Promise<string | null> {
    const response = await fetch(`${SERVER_URL}/skins/` + playerUUID);
    if (!response.ok) return null;

    const data = await response.json() as PlayerData;

    const texture = data.properties.find(property => property.name === "textures");
    if (!texture?.value) return null;

    try {
        const decoded = JSON.parse(atob(texture.value)) as TexturesObject;
        return decoded.textures.SKIN?.url ?? null;
    } catch (error) {
        console.error("Failed to decode Minecraft texture", error);
        return null;
    }
}

const verificationInputs = setupVerificationInput();
verificationInputs.forEach((input) => {
    input.addEventListener("input", () => verificationLogic(verificationInputs, loginDialog));
});

handleWebSocket();

function handleWebSocket() {
    const connectWebSocketReturn = connectWebSocket();
    if (!connectWebSocketReturn.valid
        || !connectWebSocketReturn.socket
        || !connectWebSocketReturn.profile
        || !connectWebSocketReturn.sessionID) {
        const dialog = document.getElementById("login") as HTMLDialogElement;
        dialog.showModal();
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
                socket.emit("chatMessage", createMessage(text, sessionID))
                messageInput.value = "";
            }
        }

        sendBtn.addEventListener("click", sendMessage);
        messageInput.addEventListener("keydown", e => e.key === "Enter" && sendMessage());
    }
}
