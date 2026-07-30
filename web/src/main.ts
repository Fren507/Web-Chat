import {setupVerificationInput, verificationLogic,} from "./verificationCode.ts";
import "./assets/css/minecraft.css";
import "./assets/css/style.css";
import "./assets/css/code.css";
import {connectWebSocket, createMessage} from "./websocket.ts";

const SERVER_URL = import.meta.env.VITE_SERVER_URL;

const verificationCodeHtml = `
<div class="verification-code align-center">
    ${Array.from(
    {length: 3},
    (_, group) => `
            <div class="verification-group">
                ${Array.from(
        {length: 4},
        (_, index) => `
                        <input
                            class="verification-character form-input"
                            type="text"
                            maxlength="1"
                            inputmode="text"
                            autocomplete="off"
                            spellcheck="false"
                            data-index="${group * 4 + index}"
                            aria-label="Verifizierung Zeichen ${group * 4 + index + 1}"
                        />
                    `,
    ).join("")}
            </div>
        `,).join('<span class="separator">-</span>')}
</div>
`;

// Render UI layout
document.body.classList.add("bg-primary", "text-secondary");
document.querySelector<HTMLDivElement>("#app")!.innerHTML = /*html*/ `
    <nav class="navbar" id="navbar">
        <a class="navbar-brand" href="/"> Web Chat </a>

        <ol class="navbar-nav">
            <li><a href="/">Chat</a></li>
            <li><a href="/info">Info</a></li>
        </ol>
    </nav>

    <!--suppress CssUnresolvedCustomProperty -->
    <main
            id="content"
            style="min-height: calc(100dvh - var(--navbar-height, 57px))"
    ></main>

    <footer class="bg-secondary text-white pl-xs text-center">
        <div class="p-md">
            <h3>Web Chat</h3>

            <p>Minecraft-integrierter Webchat mit Verifizierung über deinen Server.</p>

            <hr class="no-padding"/>

            <div class="grid">
                <div class="col-md-4">
                    <h5>Verbindung</h5>

                    <p id="connection-status">Nicht verbunden</p>
                </div>

                <div class="col-md-4">
                    <h5>Server</h5>

                    <p>Minecraft Bridge</p>
                </div>

                <div class="col-md-4">
                    <h5>Version</h5>

                    <p>Web Chat 1.0</p>
                </div>
            </div>
        </div>

        ©2026 Web Chat
    </footer>`;

const content = document.getElementById("content")! as HTMLDivElement;
const navbar = document.getElementById("navbar")! as HTMLElement;

const navbarHeight = navbar.getBoundingClientRect().height;
document.body.style.setProperty("--navbar-height", navbarHeight + "px");

if (window.location.pathname == "/") {
    content.innerHTML = `
    <div class="container py-lg">
        <div class="card">
        <div class="card-header">Minecraft Web Chat</div>
        
        <div class="card-body">
            <div id="chat-box"></div>
        
            <div class="form-group mt-md ">
                <div class="row" style="display: flex; align-items: center">
                    <input
                        type="text"
                        id="message-input"
                        class="form-input"
                        placeholder="Nachricht eingeben..."
                        disabled
                    />
                    <button id="send-btn" class="button button-primary mr-0 ml-sm" disabled>
                        Senden
                    </button>
                </div>
                </div>
            </div>
        </div>
    
        <dialog id="login" closedby="none" class="modal">
            <h2 class="modal-header">Verifikation</h2>
        
            <div class="modal-body">
                <p>Um diesen Chat nutzen zu können, müssen Sie sich anmelden.</p>
    
                <details>
                    <summary>Wie verifiziere ich mich?</summary>
                    <ol>
                        <li>Starten Sie Minecraft und betreten Sie den Server.</li>
                        
                        <li>
                            Geben Sie
                            <code><span class="punctuation">/</span><span class="keyword">verify</span></code>
                            im Minecraft-Chat
                            <code><span class="punctuation">(</span><span class="keyword">T</span><span class="punctuation">)</span></code>
                            ein.
                        </li>
                        
                        <li>
                            Sie erhalten einen Code im Format
                            <code><span class="string">XXXX-XXXX-XXXX</span></code>, 
                            welcher nach 2 Stunden abläuft.
                        </li>
                        
                        <li>Geben Sie den Code unten ein.</li>
                    </ol>
                </details>
            </div>
      
            <div class="modal-footer text-left">
                <h3>Ihr Verifizierungscode:</h3>
                ${verificationCodeHtml}
            </div>
        </dialog>
    </div>`;

    const loginDialog = document.querySelector<HTMLDialogElement>("#login")!;
    loginDialog.showModal();
    loginDialog.classList.add("modal-open");

    const sessionProfile = sessionStorage.getItem("web-chat-profile");
    if (sessionProfile) {
        loginDialog.close();
        loginDialog.classList.remove("modal-open");
        connectWebSocket();
    }

    const verificationInputs = setupVerificationInput();
    verificationInputs.forEach((input) => {
        input.addEventListener("input", () =>
            verificationLogic(verificationInputs, loginDialog),
        );
    });

    const messageInput =
        document.querySelector<HTMLInputElement>("#message-input")!;

    messageInput.addEventListener(
        "input",
        () => (messageInput.value = messageInput.value.replaceAll("§", "")),
    );

    handleWebSocket();

    function handleWebSocket() {
        const connectWebSocketReturn = connectWebSocket();
        if (
            !connectWebSocketReturn.valid ||
            !connectWebSocketReturn.socket ||
            !connectWebSocketReturn.profile ||
            !connectWebSocketReturn.sessionID
        ) {
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
                    socket.emit("chatMessage", createMessage(text, sessionID));
                    messageInput.value = "";
                }
            }

            sendBtn.addEventListener("click", sendMessage);
            messageInput.addEventListener(
                "keydown",
                (e) => e.key === "Enter" && sendMessage(),
            );
        }
    }
} else {
    content.innerHTML = `
    <div class="bg-secondary m-auto px-lg py-xl text-white error-page">
        <h1>404 – Chunk nicht gefunden</h1>
        <hr class="no-padding">
        <p>Diese Seite scheint in die Leere gefallen zu sein.</p>
        <p>Vielleicht wurde sie verschoben, gelöscht oder hat nie existiert. </p>
        <div class="mt-lg align-center">
            <a href="/" class="button button-primary">
                Zur Startseite
            </a>
        </div>
    </div>`;
}

interface PlayerData {
    id: string;
    name: string;
    properties: {
        name: string;
        value: string;
    }[];
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

export async function getPlayerSkin(
    playerUUID: string,
): Promise<string | null> {
    const response = await fetch(`${SERVER_URL}/skins/` + playerUUID);
    if (!response.ok) return null;

    const data = (await response.json()) as PlayerData;

    const texture = data.properties.find(
        (property) => property.name === "textures",
    );
    if (!texture?.value) return null;

    try {
        const decoded = JSON.parse(atob(texture.value)) as TexturesObject;
        return decoded.textures.SKIN?.url ?? null;
    } catch (error) {
        console.error("Failed to decode Minecraft texture", error);
        return null;
    }
}
