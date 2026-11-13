// Generates the PNG favicon set from src/assets/logo.png (the real,
// client-supplied logo -- a finished badge with its own white background
// and border baked in, not an abstract icon glyph).
//
// The logo (242x233) isn't quite square, so each target is produced with
// `fit: "contain"` onto a white background matching the logo's own
// backing color -- this adds a thin white bar on two edges rather than
// stretching/cropping the artwork, and keeps the logo's pixels completely
// untouched (no redrawing, no recoloring). iOS's home-screen icon
// (`apple-touch-icon`) and the two sizes the web manifest's `icons` array
// needs for Android's "add to home screen" are the only reasons PNGs are
// needed at all -- re-run this (`npm run generate:icons`) any time
// src/assets/logo.png changes.
//
// `sharp` is a devDependency used only by this script, not by the app
// itself.
import sharp from "sharp";
import { fileURLToPath } from "node:url";
import path from "node:path";

const rootDir = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const logoPath = path.join(rootDir, "src", "assets", "logo.png");

const white = { r: 255, g: 255, b: 255, alpha: 1 };

const targets = [
  ["favicon-16x16.png", 16],
  ["favicon-32x32.png", 32],
  ["apple-touch-icon.png", 180],
  ["android-chrome-192x192.png", 192],
  ["android-chrome-512x512.png", 512],
];

for (const [filename, size] of targets) {
  const outPath = path.join(rootDir, "public", filename);
  await sharp(logoPath)
    .resize(size, size, { fit: "contain", background: white })
    .png()
    .toFile(outPath);
  console.log(`wrote ${filename} (${size}x${size})`);
}
