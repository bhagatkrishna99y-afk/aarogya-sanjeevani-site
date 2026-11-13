/**
 * WCAG 2 contrast ratio, computed from first principles (relative
 * luminance formula from the spec) -- not estimated, not borrowed from a
 * design tool's preview. docs/design-tokens.md's numbers and the live
 * /styleguide page both come from this same function, so they can't
 * silently drift apart from each other without one of them visibly
 * being wrong.
 */
function hexToRgb(hex: string): [number, number, number] {
  const n = parseInt(hex.replace("#", ""), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

function relativeLuminance([r, g, b]: [number, number, number]): number {
  const toLinear = (channel: number) => {
    const c = channel / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  };
  const [rl, gl, bl] = [r, g, b].map(toLinear) as [number, number, number];
  return 0.2126 * rl + 0.7152 * gl + 0.0722 * bl;
}

export function contrastRatio(hexA: string, hexB: string): number {
  const lumA = relativeLuminance(hexToRgb(hexA));
  const lumB = relativeLuminance(hexToRgb(hexB));
  const lighter = Math.max(lumA, lumB);
  const darker = Math.min(lumA, lumB);
  return (lighter + 0.05) / (darker + 0.05);
}

export type ContrastBar = 4.5 | 3;

export function passesAA(ratio: number, bar: ContrastBar): boolean {
  return ratio >= bar;
}

export function formatRatio(ratio: number): string {
  return `${ratio.toFixed(2)}:1`;
}
