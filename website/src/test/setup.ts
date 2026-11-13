import "@testing-library/jest-dom/vitest";

// jsdom implements no layout engine at all and doesn't define
// ResizeObserver -- Header.tsx uses a real one in the browser to keep
// `--header-height` in sync with the header's actual rendered height.
// This minimal stub (never fires, since nothing in jsdom triggers a
// resize) is enough for components that merely construct and
// observe/disconnect one without needing it to ever actually report a
// size change.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}

if (typeof globalThis.ResizeObserver === "undefined") {
  globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver;
}
