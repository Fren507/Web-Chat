// Ein globaler Cache für bereits gerenderte Spieler-Köpfe (Key: UUID oder playerName, Value: Canvas)
const headCache = new Map<string, HTMLCanvasElement>();

export async function getMessageNode(
    playerName: string,
    message: string,
    playerUUID: string[],
    serverUrl: string
): Promise<HTMLElement> {
    // Haupt-Container
    const messageElement = document.createElement("div");
    messageElement.classList.add("chat-message");

    // Container für die Spieler-Köpfe
    const playerHeadContainer = document.createElement("div");
    playerHeadContainer.classList.add("playerHead");

    // Für jede UUID einen Kopf erstellen
    for (const uuid of playerUUID) {
        const headImg = document.createElement("img");

        headImg.alt = `${playerName} Kopf`;
        headImg.width = 32;
        headImg.height = 32;

        try {
            const canvas = await loadOrFetchHead(uuid, serverUrl);
            headImg.src = canvas.toDataURL("image/png");
        } catch (error) {
            console.error(`Kopf ${uuid} konnte nicht geladen werden:`, error);
        }

        playerHeadContainer.appendChild(headImg);
    }

    // Nachrichten-Container
    const messageContainer = document.createElement("div");
    messageContainer.classList.add("message");

    // Nachricht als HTML übernehmen
    messageContainer.innerHTML = message;

    // Zusammenbauen
    messageElement.append(
        playerHeadContainer,
        messageContainer
    );

    return messageElement;
}


/**
 * Hilfsfunktion: Prüft den Cache oder lädt den Skin genau EINMAL pro Spieler.
 */
async function loadOrFetchHead(playerUUID: string, serverUrl: string): Promise<HTMLCanvasElement> {
    // Wenn der Kopf schon im Cache liegt, sofort zurückgeben (kein erneutes Fetchen!)
    if (headCache.has(playerUUID)) {
        return headCache.get(playerUUID)!;
    }

    // Ansonsten einmalig laden und rendern
    try {
        const response = await fetch(`${serverUrl}api/skins/${playerUUID}`);
        if (!response.ok) throw new Error("Skin konnte nicht geladen werden");

        const profile = await response.json();
        const textureProperty = profile.properties?.find((p: any) => p.name === "textures");

        if (!textureProperty?.value) throw new Error("Keine Texturen gefunden");

        const decoded = JSON.parse(atob(textureProperty.value));
        const skinUrl = decoded.textures.SKIN?.url;

        if (!skinUrl) throw new Error("Skin-URL nicht gefunden");

        // Kopf auf Canvas rendern (inkl. beider Layer)
        const canvas = await renderHeadWithBothLayers(skinUrl);

        // Im Cache speichern für alle zukünftigen Nachrichten dieses Spielers
        headCache.set(playerUUID, canvas);

        return canvas;
    } catch (error) {
        console.error(`Fehler beim Laden des Kopfes für UUID ${playerUUID}:`, error);

        // Fallback: Leeres 8x8 Canvas zurückgeben, damit nichts crasht
        const fallbackCanvas = document.createElement("canvas");
        fallbackCanvas.width = 64;
        fallbackCanvas.height = 64;
        return fallbackCanvas;
    }
}

/**
 * Hilfsfunktion zum Zeichnen der beiden Layer (Basis + Helm)
 */
async function renderHeadWithBothLayers(skinUrl: string): Promise<HTMLCanvasElement> {
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.crossOrigin = "anonymous";

        img.onload = () => {
            const canvas = document.createElement("canvas");
            canvas.width = 64;
            canvas.height = 64;

            const ctx = canvas.getContext("2d");
            if (!ctx) {
                reject(new Error("Kein Canvas-Kontext"));
                return;
            }

            ctx.imageSmoothingEnabled = false;

            // 1. Schicht: Innerer Kopf
            ctx.drawImage(img, 8, 8, 8, 8, 0, 0, 64, 64);
            // 2. Schicht: Helm / Overlay
            ctx.drawImage(img, 40, 8, 8, 8, 0, 0, 64, 64);

            resolve(canvas);
        };

        img.onerror = () => reject(new Error("Bild konnte nicht geladen werden"));
        img.src = skinUrl;
    });
}