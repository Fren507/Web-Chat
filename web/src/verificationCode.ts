import {handleWebSocket,} from "./websocket.ts";

const SERVER_URL = import.meta.env.VITE_SERVER_URL;

export function setupVerificationInput(): HTMLInputElement[] {
    const inputs = [
        ...document.querySelectorAll<HTMLInputElement>(".verification-character"),
    ];

    for (const [index, input] of inputs.entries()) {
        input.addEventListener("input", () => {
            const userInput = input.value.toUpperCase().replace(/[^A-Z0-9]/g, "");
            if (userInput.length != 1) {
                input.value = "";
                return;
            }

            input.value = input.value.toUpperCase().replace(/[^A-Z0-9]/g, "");

            if (input.value && index < inputs.length - 1) {
                inputs[index + 1].focus();
            }
        });

        input.addEventListener("keydown", (event) => {
            if (event.key === "Backspace" && input.value === "" && index > 0) {
                inputs[index - 1].focus();
            }

            if (event.key === "ArrowLeft" && index > 0) {
                event.preventDefault();
                inputs[index - 1].focus();
            }

            if (event.key === "ArrowRight" && index < inputs.length - 1) {
                event.preventDefault();
                inputs[index + 1].focus();
            }

            if (event.key === "ArrowUp") {
                event.preventDefault();
                inputs[inputs.length - 1].focus();
            }

            if (event.key === "ArrowDown") {
                event.preventDefault();
                inputs[0].focus();
            }
        });
    }

    const container = document.querySelector<HTMLDivElement>(".verification-code")!;
    const loginDialog = document.querySelector<HTMLDialogElement>("#login")!;

    container.addEventListener("paste", (event: ClipboardEvent) => {
        event.preventDefault();

        // window.clipboardData weglassen, nur das moderne event.clipboardData nutzen:
        const text: string = (event.clipboardData?.getData("text") || "")
            .toUpperCase()
            .replace(/[^A-Z0-9]/g, "")
            .slice(0, 12);

        if (!text) return;

        text.split("").forEach((char, index) => {
            inputs[index].value = char;
        });

        inputs[Math.min(text.length, 12) - 1].focus();

        verificationLogic(inputs, loginDialog);
    });

    return inputs;
}

export interface VerificationProfile {
    profileUUID: string;
    playerUUID: string;
    expiresAt: number;
    createdAt: number;
    sessionID: string;
    username: string;
    valid: boolean;
}

export interface VerificationReturn {
    valid: boolean;
    profile: VerificationProfile | null;
}

export function verificationLogic(verificationInputs: HTMLInputElement[], loginDialog: HTMLDialogElement) {
    const verificationCode = verificationInputs.map((i) => i.value).join("");

    if (verificationCode.length !== 12) return;

    verifyVerificationCode(verificationCode).then((verificationReturn) => {
        if (verificationReturn.valid && verificationReturn.profile) {
            loginDialog.close();
            loginDialog.classList.remove("modal-open")
            sessionStorage.setItem("web-chat-session", verificationReturn.profile.sessionID);
            sessionStorage.setItem("web-chat-profile", JSON.stringify(verificationReturn.profile));
            handleWebSocket();
        } else {
            console.log("Invalid verification code");
        }
    });
}

export async function verifyVerificationCode(
    token: string,
): Promise<VerificationReturn> {
    console.log(token);
    if (!token || token.length !== 12) return {valid: false, profile: null};
    const response = await fetch(`${SERVER_URL}/verify`, {
        method: "POST",
        body: token,
    });

    if (!response.ok) return {valid: false, profile: null};


    const data = await response.json() as VerificationReturn;

    console.log(response, data);

    return data;
}
