import {setupVerificationInput, verificationLogic,} from "./verificationCode.ts";
import {handleWebSocket} from "./websocket.ts";
import "./assets/css/minecraft.css";
import "./assets/css/style.css";
import "./assets/css/code.css";

const serverUrl = new URL(window.location.origin);
export {serverUrl,};

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
        `,
).join('<span class="separator">-</span>')}
</div>
`;

// Render UI layout
document.body.classList.add("bg-primary", "text-secondary");
document.querySelector<HTMLDivElement>("#app")!.innerHTML = /*html*/ `
    <nav class="navbar" id="navbar">
        <a class="navbar-brand" href="/"> Web-Chat </a>

        <ol class="navbar-nav">
            <li><a href="/">Chat</a></li>
            <li><a href="/info">Info</a></li>
            <li><a href="/logoff">Abmelden</a></li>
        </ol>
    </nav>

    <!--suppress CssUnresolvedCustomProperty -->
    <main
            id="content"
            style="min-height: calc(100dvh - var(--navbar-height, 57px))"
    ></main>

    <footer class="bg-secondary text-primary pl-xs text-center pb-sm">
        <div class="p-md">
            <h3>Web-Chat</h3>

            <p>Minecraft-integrierter Webchat mit Verifizierung über deinen Server.</p>

            <hr class="no-padding"/>

            <div class="grid">
                <div class="col-md-4">
                    <h5>Server</h5>

                    <p>Minecraft Bridge</p>
                </div>
                
                <div class="col-md-4">
                    <h5>Über diesen Mod</h5>
                    
                    <a href="/info" class="text-primary">Information (Klick mich!)</a>
                </div>

                <div class="col-md-4">
                    <h5>Version</h5>

                    <p>Web-Chat 1.0</p>
                </div>
            </div>
        </div>

        ©2026 Jason Frenzel
    </footer>`;

const content = document.getElementById("content")! as HTMLDivElement;
const navbar = document.getElementById("navbar")! as HTMLElement;

const navbarHeight = navbar.getBoundingClientRect().height;
document.body.style.setProperty("--navbar-height", navbarHeight + "px");

if (window.location.pathname == "/") {
    content.innerHTML = `
    <div class="container py-sm">

        <!-- Chat -->
        <section class="card mb-sm " style="height: calc(100vh - var(--navbar-height, 57px) - 2 * 0.5rem); display: flex; flex-direction: column">

            <div class="card-header">
                <div class="row text-secondary" style="display: flex; align-items: start; justify-content: space-between;">
                    <div>
                        <h2 style="margin-bottom: 0;">Server-Chat</h2>
                        <small class="hidden sm-block">Minecraft WebSocket</small>
                    </div>

                    <small id="connection-status">
                        Offline
                    </small>
                </div>
            </div>

            <div class="card-body" style="display: flex; flex-direction: column; flex: 1; overflow-y: auto;">

                <!-- Chat messages -->
                <div
                    id="chat-box"
                    style="
                        min-height: 320px;
                        overflow-y: scroll;
                        flex: 1;
                    "
                >
                    <div class="card" id="no-messages-card">
                        <div class="card-body text-center">
                            <h3>Noch keine Nachrichten</h3>

                            <p>
                                Sobald du verbunden bist, erscheinen hier
                                Nachrichten aus deinem Minecraft-Server.
                            </p>

                            <small>
                                Öffne Minecraft und halte Ausschau nach
                                neuen Nachrichten.
                            </small>
                        </div>
                    </div>
                </div>


                <!-- Message input -->
                <div class="form-group mt-md mb-0">

                    <label for="message-input">
                        Nachricht
                    </label>

                    <div
                        class="row"
                        style="
                            display: flex;
                            align-items: center;
                        "
                    >
                        <input
                            type="text"
                            id="message-input"
                            class="form-input"
                            placeholder="Nachricht an den Minecraft-Chat..."
                            autocomplete="off"
                            disabled
                        />

                        <button
                            id="send-btn"
                            class="button button-primary mr-0 ml-sm"
                            disabled
                        >
                            Senden
                        </button>
                    </div>
                </div>
            </div>
        </section>

        
        <!-- Hero -->
        <section class="card mb-lg">
            <div class="card-header">
                <h2>Chatte mit deinem Minecraft-Server.</h2>
            </div>
            
            <div class="card-body">
                <div class="grid">
                    <div class="col-8 col-md-12">
                        <p>
                            Verbinde deinen Browser direkt mit deinem
                            Minecraft-Server und empfange Nachrichten
                            in Echtzeit.
                        </p>

                        <p>
                            Keine zusätzlichen Programme, keine komplizierte
                            Einrichtung. Verifizieren, verbinden und loschatten.
                        </p>

                        <div class="mt-md">
                            <a href="/info" class="button button-primary">
                                Mehr über den Web-Chat
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </section>


        <!-- Verification dialog -->
        <dialog id="login" closedby="none" class="modal">

            <h2 class="modal-header">
                Verifi&shy;zierung
            </h2>

            <div class="modal-body">

                <p>
                    Um den Web-Chat nutzen zu können, musst du dich
                    über deinen Minecraft-Server verifizieren.
                </p>

                <details open>
                    <summary>
                        Wie funktioniert die Verifizierung?
                    </summary>

                    <ol>
                        <li>
                            Starte Minecraft und betrete den Server.
                        </li>

                        <li>
                            Öffne den Minecraft-Chat und führe
                            <code><span class="punctuation">/</span><span class="keyword">verify</span></code>
                            aus.
                        </li>

                        <li>
                            Minecraft zeigt dir einen Code im Format
                            <code><span class="string">XXXX-XXXX-XXXX</span></code>
                            an.
                        </li>

                        <li>
                            Gib diesen Code unten ein.
                        </li>
                    </ol>

                    <p>
                        <small>
                            Der Code ist für 2 Stunden gültig.
                        </small>
                    </p>
                </details>

            </div>

            <div class="modal-footer text-left">

                <h3>Verifi&shy;zierungs&shy;code</h3>

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
        handleWebSocket();
    }

    const verificationInputs = setupVerificationInput();

    verificationInputs.forEach((input) => {
        input.addEventListener("input", () =>
            verificationLogic(verificationInputs, loginDialog),
        );
    });

    const messageInput =
        document.querySelector<HTMLInputElement>("#message-input")!;

    messageInput.addEventListener("input", () => {
        messageInput.value = messageInput.value.replaceAll("§", "");
    });
} else if (window.location.pathname == "/info") {
    content.innerHTML = `
    <div class="container py-lg">

        <!-- Header -->
        <div class="card mb-lg">
            <div class="card-header">
                <h2>Minecraft Web-Chat</h2>
            </div>

            <div class="card-body">
                <p>
                    Der <strong>Minecraft Web-Chat</strong> ermöglicht es dir,
                    direkt über deinen Browser mit Spielern auf deinem Minecraft-Server
                    zu kommunizieren.
                </p>

                <p>
                    Die Verbindung erfolgt in Echtzeit. Nachrichten aus dem
                    Minecraft-Chat werden im Browser angezeigt und Nachrichten
                    aus dem Web-Chat werden direkt an den Server weitergeleitet.
                </p>

                <hr class="no-padding"/>

                <div class="grid">
                    <div class="col-4 col-md-12">
                        <strong>⚡ Echtzeit</strong>
                        <p>
                            Nachrichten werden ohne manuelles Aktualisieren
                            über eine WebSocket-Verbindung übertragen.
                        </p>
                    </div>

                    <div class="col-4 col-md-12">
                        <strong>🔐 Verifiziert</strong>
                        <p>
                            Der Zugriff auf den Web-Chat erfolgt über einen
                            Verifizierungscode aus Minecraft.
                        </p>
                    </div>

                    <div class="col-4 col-md-12">
                        <strong>⛏ Minecraft</strong>
                        <p>
                            Das Interface orientiert sich bewusst am Look
                            und Gefühl von Minecraft.
                        </p>
                    </div>
                </div>
            </div>
        </div>


        <!-- Erste Schritte -->
        <div class="card mb-lg">
            <div class="card-header">
                <h2>Erste Schritte</h2>
            </div>

            <div class="card-body">
                <ol>
                    <li>
                        <strong>Betrete den Minecraft-Server.</strong>
                        <p>
                            Du musst dich zunächst ganz normal auf dem
                            Minecraft-Server befinden.
                        </p>
                    </li>

                    <li>
                        <strong>Fordere einen Verifizierungscode an.</strong>
                        <p>
                            Öffne den Minecraft-Chat und führe
                            <code><span class="punctuation">/</span><span class="keyword">verify</span></code> aus.
                        </p>
                    </li>

                    <li>
                        <strong>Öffne den Web-Chat.</strong>
                        <p>
                            Gib den angezeigten Verifizierungscode auf
                            der Startseite des Web-Chats ein.
                        </p>
                    </li>

                    <li>
                        <strong>Loschatten!</strong>
                        <p>
                            Nach erfolgreicher Verifizierung kannst du
                            Nachrichten senden und Nachrichten vom
                            Minecraft-Server empfangen.
                        </p>
                    </li>
                </ol>

                <div class="card mt-md">
                    <div class="card-body">
                        <strong>Hinweis:</strong>
                        <p>
                            Der Verifizierungscode ist zeitlich begrenzt.
                            Wenn dein Code abgelaufen ist, kannst du mit
                            <code><span class="punctuation">/</span><span class="keyword">verify</span></code> einen neuen Code erzeugen.
                        </p>
                    </div>
                </div>
            </div>
        </div>


        <!-- Features -->
        <div class="card mb-lg">
            <div class="card-header">
                <h2>Funktionen</h2>
            </div>

            <div class="card-body">
                <div class="grid">

                    <div class="col-6 col-md-12">
                        <h3>Echtzeit-Chat</h3>
                        <p>
                            Der Web-Chat verwendet WebSockets, sodass
                            Nachrichten sofort zwischen Browser und
                            Minecraft-Server übertragen werden.
                        </p>
                    </div>

                    <div class="col-6 col-md-12">
                        <h3>Server-Nachrichten</h3>
                        <p>
                            Auch Ereignisse vom Minecraft-Server können
                            im Web-Chat angezeigt werden, beispielsweise
                            wenn ein Spieler den Server betritt oder verlässt.
                        </p>
                    </div>

                    <div class="col-6 col-md-12">
                        <h3>Verifizierung</h3>
                        <p>
                            Der Zugriff wird über einen Code authentifiziert,
                            der direkt von Minecraft aus erzeugt wird.
                        </p>
                    </div>

                    <div class="col-6 col-md-12">
                        <h3>Minecraft-Design</h3>
                        <p>
                            Farben, Buttons, Karten und Typografie sind an
                            die klassische Minecraft-Oberfläche angelehnt.
                        </p>
                    </div>

                </div>
            </div>
        </div>


        <!-- FAQ -->
        <div class="card mb-lg">
            <div class="card-header">
                <h2>Häufige Fragen</h2>
            </div>

            <div class="card-body">

                <details class="mb-sm">
                    <summary>
                        <strong>Wie lange ist mein Verifizierungscode gültig?</strong>
                    </summary>
                    <p class="mt-xs">
                        Ein Verifizierungscode ist für <strong>2 Stunden</strong>
                        gültig. Danach musst du mit <code><span class="punctuation">/</span><span class="keyword">verify</span></code>
                        einen neuen Code erzeugen.
                    </p>
                </details>

                <details class="mb-sm">
                    <summary>
                        <strong>Kann ich den Web-Chat ohne Minecraft verwenden?</strong>
                    </summary>
                    <p class="mt-xs">
                        Nein. Für die Verifizierung benötigst du Zugriff auf
                        den Minecraft-Server und musst dort den
                        <code><span class="punctuation">/</span><span class="keyword">verify</span></code>-Befehl ausführen.
                    </p>
                </details>

                <details class="mb-sm">
                    <summary>
                        <strong>Warum kann ich keine Nachrichten senden?</strong>
                    </summary>
                    <p class="mt-xs">
                        Überprüfe zuerst, ob du erfolgreich verifiziert bist
                        und ob deine WebSocket-Verbindung aktiv ist.
                    </p>
                </details>

                <details class="mb-sm">
                    <summary>
                        <strong>Was passiert, wenn die Verbindung abbricht?</strong>
                    </summary>
                    <p class="mt-xs">
                        Der Web-Chat versucht, die Verbindung zum Server
                        wiederherzustellen. Während keine Verbindung besteht,
                        können Nachrichten möglicherweise nicht gesendet werden.
                    </p>
                </details>

                <details>
                    <summary>
                        <strong>Kann ich den Web-Chat auf dem Smartphone verwenden?</strong>
                    </summary>
                    <p class="mt-xs">
                        Ja. Der Web-Chat kann direkt im Browser verwendet werden,
                        sofern der Server erreichbar ist und dein Gerät über
                        eine aktive Internetverbindung verfügt.
                    </p>
                </details>

            </div>
        </div>


        <!-- Projekt -->
        <div class="grid mb-lg">

            <div class="col-6 col-md-12">
                <div class="card">
                    <div class="card-header">
                        <h2>Open Source</h2>
                    </div>

                    <div class="card-body">
                        <p>
                            Der Minecraft Web-Chat und die dazugehörige
                            Minecraft-Mod können auf GitHub eingesehen werden.
                        </p>

                        <p>
                            Dort findest du den Quellcode, die Entwicklung
                            des Projekts und weitere technische Informationen.
                        </p>

                        <a href="#" class="button button-primary">
                            Auf GitHub ansehen
                        </a>
                    </div>
                </div>
            </div>


            <div class="col-6 col-md-12">
                <div class="card">
                    <div class="card-header">
                        <h2>Modrinth</h2>
                    </div>

                    <div class="card-body">
                        <p>
                            Die Minecraft-Mod ist außerdem auf Modrinth
                            verfügbar. Dort findest du Informationen zur
                            Installation, Versionen und Downloads.
                        </p>

                        <a href="#" class="button button-secondary">
                            Auf Modrinth ansehen
                        </a>
                    </div>
                </div>
            </div>

        </div>


        <!-- Navigation -->
        <div class="align-center grid mt-xl">
            <a href="/" class="button button-tertiary">
                Zurück zum Chat
            </a>
        </div>

    </div>`;
} else if (window.location.pathname == "/logoff") {
    if (sessionStorage.getItem("web-chat-session")) {
        sessionStorage.removeItem("web-chat-session");
        sessionStorage.removeItem("web-chat-profile");
    }
    window.location.href = "/";
} else {
    content.innerHTML = `
    <div class="bg-secondary m-auto px-lg py-xl text-primary error-page">
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
