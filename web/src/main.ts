import {connectWebSocket} from "./websocket.ts";
import {setupVerificationInput, verifyVerificationCode,} from "./verificationCode.ts";
import "./assets/css/style.css";
import "./assets/css/code.css"

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

  <button command="show-modal" commandfor="my-dialog">Open dialog</button> -->

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

let verificationCode = "";
const verificationInputs = setupVerificationInput();
verificationInputs.forEach((input) => {
    input.addEventListener("input", () => {
        verificationCode = verificationInputs.map((i) => i.value).join("");
        verifyVerificationCode(verificationCode).then((verificationReturn) => {
            if (verificationReturn.valid) {
                loginDialog.close();
                connectWebSocket();
            } else {
                console.log("Invalid verification code");
            }
        });
    });
});

// // Initialize WebSocket connection
// const socket = connectWebSocket();

// const messageInput =
//   document.querySelector<HTMLInputElement>("#message-input")!;
// const sendBtn = document.querySelector<HTMLButtonElement>("#send-btn")!;

// function sendMessage() {
//   const text = messageInput.value.trim();
//   if (text !== "" && socket.readyState === WebSocket.OPEN) {
//     socket.send(text);
//     messageInput.value = "";
//   }
// }

// // Send on button click
// sendBtn.addEventListener("click", sendMessage);

// // Send on Pressing 'Enter'
// messageInput.addEventListener("keydown", (e) => {
//   if (e.key === "Enter") {
//     sendMessage();
//   }
// });
