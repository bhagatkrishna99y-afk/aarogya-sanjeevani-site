import { useEffect, type RefObject } from "react";

const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

function getFocusable(container: HTMLElement): HTMLElement[] {
  // Deliberately not filtering on `offsetParent`/layout to exclude
  // hidden elements: jsdom does no layout at all, so `offsetParent` is
  // always null there regardless of real visibility, which would make
  // this trap silently find zero focusable elements under test. If a
  // future caller's trapped container can contain a genuinely hidden
  // focusable element, give that element `tabindex="-1"` (or
  // `disabled`) itself rather than relying on this hook to infer
  // visibility from layout.
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR));
}

/**
 * Traps Tab/Shift+Tab focus cycling inside `containerRef` while `active`
 * is true, and calls `onEscape` when Escape is pressed (it's the
 * caller's job to decide what Escape does -- normally closing whatever
 * this is trapping focus inside, which also naturally lets Tab escape
 * again once `active` turns false). Doesn't move focus itself on
 * activate/deactivate -- callers that need "focus the first item when
 * this opens" or "restore focus to the trigger when this closes" do
 * that themselves, since the right initial/restore target is
 * context-specific.
 */
export function useFocusTrap(
  containerRef: RefObject<HTMLElement | null>,
  active: boolean,
  onEscape?: () => void,
) {
  useEffect(() => {
    if (!active) return;
    const container = containerRef.current;
    if (!container) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        onEscape?.();
        return;
      }
      if (event.key !== "Tab") return;

      const focusable = getFocusable(container!);
      if (focusable.length === 0) return;
      const first = focusable[0]!;
      const last = focusable[focusable.length - 1]!;

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [active, containerRef, onEscape]);
}
