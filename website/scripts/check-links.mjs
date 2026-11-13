#!/usr/bin/env node
/**
 * Crawls the built site with a real browser and checks every <a href> it
 * finds: internal links must resolve to a real route (not the 404 page),
 * external links get a live HTTP check, and mailto:/tel: links just get
 * a well-formedness check. A plain static-HTML link checker can't do this
 * -- dist/index.html has no <a> tags at all; every one of them is
 * rendered by React Router only after JS runs.
 *
 * Usage: npm run check:links (builds, serves dist/, crawls, then exits
 * non-zero if anything is broken).
 */
import { chromium } from "@playwright/test";
import { spawn } from "node:child_process";
import { setTimeout as sleep } from "node:timers/promises";

const BASE_URL = "http://localhost:4175";
const PAGES_TO_CRAWL = ["/", "/services", "/about", "/contact", "/privacy-policy", "/terms"];
// Known good 404 heading text -- if an internal link lands here, it's broken.
const NOT_FOUND_HEADING = "Page not found";

async function waitForServer(url, attempts = 60) {
  for (let i = 0; i < attempts; i++) {
    try {
      const res = await fetch(url);
      if (res.ok) return;
    } catch {
      // not up yet
    }
    await sleep(500);
  }
  throw new Error(`Server at ${url} never came up`);
}

async function checkExternalLink(url) {
  try {
    const res = await fetch(url, { method: "HEAD", redirect: "follow" });
    if (res.ok) return null;
    // Some servers reject HEAD; retry with GET before giving up.
    const getRes = await fetch(url, { method: "GET", redirect: "follow" });
    if (getRes.ok) return null;
    return `HTTP ${getRes.status}`;
  } catch (err) {
    return err instanceof Error ? err.message : String(err);
  }
}

async function main() {
  console.log("Building...");
  await run("npm", ["run", "build"]);

  console.log("Starting preview server...");
  const preview = spawn("npm", ["run", "preview", "--", "--port", "4175", "--strictPort"], {
    shell: true,
    stdio: "ignore",
  });

  const problems = [];
  try {
    await waitForServer(BASE_URL + "/");

    const browser = await chromium.launch();
    const page = await browser.newPage();

    const seenExternal = new Set();

    for (const path of PAGES_TO_CRAWL) {
      await page.goto(BASE_URL + path, { waitUntil: "networkidle" });
      const links = await page.$$eval("a[href]", (as) => as.map((a) => a.getAttribute("href")));

      for (const href of links) {
        if (!href || href.startsWith("#")) continue;

        if (href.startsWith("mailto:")) {
          if (!/^mailto:[^@\s]+@[^@\s]+\.[^@\s]+/.test(href)) {
            problems.push(`${path}: malformed mailto link "${href}"`);
          }
          continue;
        }
        if (href.startsWith("tel:")) {
          if (!/^tel:\+?[0-9 ()-]{7,}$/.test(href)) {
            problems.push(`${path}: malformed tel link "${href}"`);
          }
          continue;
        }

        if (href.startsWith("http://") || href.startsWith("https://")) {
          if (href.startsWith(BASE_URL)) {
            // An absolute link back to our own preview origin -- treat
            // like internal below instead of a live external fetch.
          } else {
            if (seenExternal.has(href)) continue;
            seenExternal.add(href);
            const problem = await checkExternalLink(href);
            if (problem) problems.push(`${path}: external link "${href}" failed (${problem})`);
            continue;
          }
        }

        // Internal link: navigate and confirm it isn't the 404 page.
        const internalPath = href.replace(BASE_URL, "");
        const checkPage = await browser.newPage();
        await checkPage.goto(BASE_URL + internalPath, { waitUntil: "networkidle" });
        const isNotFound = await checkPage
          .getByRole("heading", { name: NOT_FOUND_HEADING })
          .isVisible()
          .catch(() => false);
        await checkPage.close();
        if (isNotFound) {
          problems.push(`${path}: internal link "${href}" resolves to the 404 page`);
        }
      }
    }

    await browser.close();
  } finally {
    preview.kill();
  }

  if (problems.length > 0) {
    console.error(`\nFound ${problems.length} broken link(s):\n`);
    for (const p of problems) console.error(`  - ${p}`);
    process.exit(1);
  }

  console.log("No broken links found.");
}

function run(cmd, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, { shell: true, stdio: "inherit" });
    child.on("exit", (code) =>
      code === 0 ? resolve() : reject(new Error(`${cmd} exited ${code}`)),
    );
  });
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
