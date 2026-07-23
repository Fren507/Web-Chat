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
                inputs[11].focus();
            }

            if (event.key === "ArrowDown") {
                event.preventDefault();
                inputs[0].focus();
            }
        });
    }

    const container = document.querySelector<HTMLDivElement>(".verification-code")!;

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
    });

    return inputs;
}

export interface VerificationProfile {
    profileUUID: string;
    playerUUID: string;
    expiresAt: number;
    createdAt: number;
    token: string;
    username: string;
    valid: boolean;
}

export interface VerificationReturn {
    valid: boolean;
    profile: VerificationProfile | null;
}

export async function verifyVerificationCode(
    token: string,
): Promise<VerificationReturn> {
    console.warn(token);
    if (!token || token.length !== 12) return {valid: false, profile: null};
    const response = await fetch("http://100.109.207.66:8080/verify", {
        method: "POST",
        body: token,
    });

    if (!response.ok) return {valid: false, profile: null};


    const data = await response.json() as VerificationReturn;

    console.warn(response, data);

    return data;
}
