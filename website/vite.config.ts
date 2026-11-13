import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
// `defineConfig` from 'vitest/config' re-exports Vite's own, augmented with
// the `test` field below -- a drop-in replacement, not a second config tool.
// `loadEnv` isn't re-exported there, so it comes from plain `vite` instead.
import { defineConfig, type Plugin } from "vitest/config";
import { loadEnv } from "vite";

const srcDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "src");

/**
 * Injects <link rel="preload"> for the two font files actually needed to
 * render the first paint without a flash of unstyled/fallback text: the H1
 * weight (Figtree 700) and the body weight (Noto Sans 400) -- see
 * src/styles/index.css's font-family rules. Every other weight imported
 * there is used further down the page and doesn't need to block on being
 * discovered that early.
 *
 * This can't be a static <link> in index.html: Fontsource's CSS emits the
 * real @font-face src as a content-hashed asset, so the exact filename
 * only exists after Vite's build. Reading it back out of the finished
 * bundle (via transformIndexHtml's `bundle` argument, build-only) is the
 * standard way to preload a hashed asset without hand-maintaining a path
 * that would go stale on every font-related change.
 */
function preloadCriticalFonts(): Plugin {
  const criticalFonts = [
    /figtree-latin-700-normal.*\.woff2$/,
    /noto-sans-latin-400-normal.*\.woff2$/,
  ];

  return {
    name: "preload-critical-fonts",
    transformIndexHtml(_html, ctx) {
      if (!ctx.bundle) return [];
      const fileNames = Object.values(ctx.bundle)
        .filter((chunk) => chunk.type === "asset")
        .map((chunk) => chunk.fileName)
        .filter((fileName) => criticalFonts.some((pattern) => pattern.test(fileName)));

      return fileNames.map((fileName) => ({
        tag: "link",
        injectTo: "head-prepend" as const,
        attrs: {
          rel: "preload",
          as: "font",
          type: "font/woff2",
          href: `/${fileName}`,
          crossorigin: "",
        },
      }));
    },
  };
}

/**
 * `public/robots.txt` and `public/sitemap.xml` are plain static files --
 * Vite copies them into `dist/` byte-for-byte, with none of the
 * %VITE_SITE_URL%-style interpolation it does for index.html. Left as-is,
 * they'd ship with the literal placeholder domain
 * ("https://example.com", matching .env.example) baked in forever,
 * silently wrong on every real deploy unless someone remembers to hand-
 * edit two files nobody usually thinks to check. Patching them in
 * `closeBundle` (runs after Vite's own copy of `public/` into `dist/`,
 * so this always has the final files to edit, not a stale pre-copy
 * snapshot) with the real `VITE_SITE_URL` fixes that at the source.
 */
function patchSeoFilesWithSiteUrl(siteUrl: string): Plugin {
  return {
    name: "patch-seo-files-with-site-url",
    apply: "build",
    closeBundle() {
      for (const file of ["robots.txt", "sitemap.xml"]) {
        const filePath = path.resolve(srcDir, "..", "dist", file);
        const contents = readFileSync(filePath, "utf-8");
        writeFileSync(filePath, contents.replaceAll("https://example.com", siteUrl));
      }
    },
  };
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "VITE_");
  return {
    plugins: [
      react(),
      tailwindcss(),
      preloadCriticalFonts(),
      patchSeoFilesWithSiteUrl(env.VITE_SITE_URL),
    ],
    resolve: {
      alias: {
        "@": srcDir,
      },
    },
    build: {
      // Vite's default (assets under 4KB get inlined as `data:` URIs)
      // breaks both hero-illustration.svg and trust-illustration.svg: its
      // encoder doesn't escape `#`, and both SVGs use hex colors
      // (`fill="#f0fdfa"` etc.), so the browser reads the first unescaped
      // `#` as a URL fragment and truncates the image data, producing a
      // broken image (confirmed by inspecting the built bundle and the
      // rendered page). The documented per-import escape hatches (`?url`
      // suffix, `new URL(path, import.meta.url)`) were tried first and
      // both failed to force a real emitted file in this project's Vite
      // 8/Rolldown build -- verified empirically by rebuilding and
      // checking dist/assets for a real .svg file each time, not assumed.
      // Disabling inlining outright is the one fix that's guaranteed to
      // work regardless of bundler-internal quirks: this project's only
      // other small assets are fonts (loaded via Fontsource's own CSS,
      // unaffected), so the cost is two extra tiny requests site-wide,
      // not a real performance regression.
      assetsInlineLimit: 0,
      modulePreload: {
        // Vite's default modulePreload behavior walks the FULL possible
        // import graph from index.html's entry point and injects a
        // <link rel="modulepreload"> for every chunk reachable through it --
        // including ones only reachable via a React.lazy() dynamic import,
        // like the admin-only `supabase` chunk. The browser's preload
        // scanner fetches those eagerly on every page load regardless of
        // route, which silently undoes the whole point of lazy-loading
        // AuthProvider in router.tsx (confirmed via Lighthouse's
        // unused-javascript audit: the supabase chunk showed up as 84%
        // unused bytes on the Home page before this). Filtering it out of
        // the auto-generated preload list is the fix -- it can still load
        // normally, on demand, once a visitor actually navigates to /admin*.
        resolveDependencies: (_filename, deps) => deps.filter((dep) => !dep.includes("supabase")),
      },
      rollupOptions: {
        output: {
          // Without this, Rollup's default heuristic merges @supabase/supabase-js
          // into the main entry chunk even though it's only ever reached through
          // the lazy /admin* route subtree (AdminAuthProvider) -- verified by
          // inspecting the built output, not assumed. Splitting it into its own
          // named chunk makes that chunk load only when a visitor actually
          // navigates to /admin*, and lets browsers cache it separately from
          // app code that changes far more often.
          manualChunks(id) {
            if (id.includes("@supabase/supabase-js")) return "supabase";
            if (/node_modules\/(react|react-dom|react-router-dom|react-router)\//.test(id)) {
              return "react-vendor";
            }
          },
        },
      },
    },
    test: {
      environment: "jsdom",
      globals: true,
      setupFiles: ["./src/test/setup.ts"],
      exclude: ["**/node_modules/**", "**/dist/**", "e2e/**"],
      // The default 5s is too tight once the full suite runs many jsdom
      // environments in parallel -- under that contention, even plain
      // synchronous tests have sporadically timed out waiting for a turn
      // on the event loop.
      testTimeout: 10000,
    },
  };
});
