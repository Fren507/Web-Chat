const COLOR_CODES: Record<string, string> = {
    '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
    '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
    '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF',
    'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF'
};

const FORMAT_STYLES: Record<string, string> = {
    'l': 'font-weight:bold;',
    'm': 'text-decoration:line-through;',
    'n': 'text-decoration:underline;',
    'o': 'font-style:italic;'
};

export function minecraftToHTML(text: string): string {
    const regex = /[§&]([0-9a-fk-or])/gi;

    let color = '';
    let formats: string[] = [];

    let result = '';
    let lastIndex = 0;

    const flush = (content: string) => {
        if (!content) return;

        const styles = [
            color,
            ...formats
        ].filter(Boolean).join('');

        if (styles) {
            result += `<span style="${styles}">${escapeHTML(content)}</span>`;
        } else {
            result += escapeHTML(content);
        }
    };

    for (const match of text.matchAll(regex)) {
        flush(text.substring(lastIndex, match.index));

        const code = match[1].toLowerCase();

        if (COLOR_CODES[code]) {
            color = `color:${COLOR_CODES[code]};`;
            formats = [];
        } else if (FORMAT_STYLES[code]) {
            formats.push(FORMAT_STYLES[code]);
        } else if (code === 'r') {
            color = '';
            formats = [];
        }

        lastIndex = (match.index ?? 0) + match[0].length;
    }

    flush(text.substring(lastIndex));

    return result;
}

function escapeHTML(text: string): string {
    return text
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}