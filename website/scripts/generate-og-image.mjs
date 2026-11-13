// Generates public/og-image.png (1200x630, the standard Open Graph /
// Twitter card size) by centering the real logo (src/assets/logo.png) on
// a plain brand-colored card. The logo already carries its own wordmark
// and tagline, so this script doesn't re-typeset "Aarogya Sanjeevani" or
// a tagline separately -- that would just duplicate what's already in the
// image. Re-run (`npm run generate:og-image`) if src/assets/logo.png
// changes.
import sharp from "sharp";
import { fileURLToPath } from "node:url";
import path from "node:path";

const rootDir = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const logoPath = path.join(rootDir, "src", "assets", "logo.png");

const WIDTH = 1200;
const HEIGHT = 630;
const MARK_HEIGHT = 440;

const card = `
<svg xmlns="http://www.w3.org/2000/svg" width="${WIDTH}" height="${HEIGHT}" viewBox="0 0 ${WIDTH} ${HEIGHT}">
  <rect width="${WIDTH}" height="${HEIGHT}" fill="#f8fafc" />
  <rect x="0" y="0" width="16" height="${HEIGHT}" fill="#0f766e" />
</svg>
`;

const mark = await sharp(logoPath)
  .resize({ height: MARK_HEIGHT, fit: "contain", background: { r: 0, g: 0, b: 0, alpha: 0 } })
  .toBuffer();
const markMeta = await sharp(mark).metadata();

const outPath = path.join(rootDir, "public", "og-image.png");
await sharp(Buffer.from(card))
  .composite([
    {
      input: mark,
      left: Math.round((WIDTH - markMeta.width) / 2),
      top: Math.round((HEIGHT - markMeta.height) / 2),
    },
  ])
  .png()
  .toFile(outPath);

console.log(`wrote og-image.png (${WIDTH}x${HEIGHT})`);
