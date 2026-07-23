export function connectWebSocket(): WebSocket {
  const socket = new WebSocket("ws://localhost:8081");

  const status = document.querySelector("#status");
  const title = document.querySelector("#title");
  const chatBox = document.querySelector("#chat-box");
  const messageInput =
    document.querySelector<HTMLInputElement>("#message-input");
  const sendBtn = document.querySelector<HTMLButtonElement>("#send-btn");

  socket.onopen = () => {
    if (status) status.textContent = "Verbunden";
    if (title) title.textContent = "WebChat";

    // Enable inputs now that we're connected!
    if (messageInput) messageInput.disabled = false;
    if (sendBtn) sendBtn.disabled = false;
  };

  socket.onmessage = (event) => {
    console.log("Server:", event.data);

    // Append received text to chat container
    if (chatBox) {
      const msgElement = document.createElement("div");
      msgElement.textContent = event.data;
      chatBox.appendChild(msgElement);
      chatBox.scrollTop = chatBox.scrollHeight; // Auto-scroll to bottom
    }
  };

  socket.onerror = () => {
    if (status) status.textContent = "Fehler";
  };

  socket.onclose = () => {
    if (status) status.textContent = "Getrennt";
    if (messageInput) messageInput.disabled = true;
    if (sendBtn) sendBtn.disabled = true;
  };

  return socket;
}
