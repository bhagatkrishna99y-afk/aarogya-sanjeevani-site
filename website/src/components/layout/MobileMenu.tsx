import { useEffect, useRef, type RefObject } from "react";
import { NavLink } from "react-router-dom";
import { X } from "lucide-react";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { useFocusTrap } from "@/lib/useFocusTrap";
import type { NavItem } from "@/content/site";

export interface MobileMenuProps {
  open: boolean;
  onClose: () => void;
  items: NavItem[];
  appHref: string;
  /** Focus returns here when the menu closes -- normally the hamburger button that opened it. */
  returnFocusRef: RefObject<HTMLElement | null>;
}

/**
 * The mobile slide-in nav panel: a real `role="dialog"`/`aria-modal`,
 * Tab/Shift+Tab trapped inside while open (`useFocusTrap`), Escape
 * closes it, and focus moves to its close button on open and back to
 * whatever opened it (`returnFocusRef`) on close.
 */
export function MobileMenu({ open, onClose, items, appHref, returnFocusRef }: MobileMenuProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const wasOpen = useRef(false);

  useFocusTrap(panelRef, open, onClose);

  useEffect(() => {
    if (open) {
      closeButtonRef.current?.focus();
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  useEffect(() => {
    // Only restore focus on an actual open -> close transition, never on
    // mount (when `open` starts false) -- otherwise this would steal
    // focus onto the hamburger button on every page load.
    if (wasOpen.current && !open) {
      returnFocusRef.current?.focus();
    }
    wasOpen.current = open;
  }, [open, returnFocusRef]);

  return (
    <>
      <div
        aria-hidden="true"
        onClick={onClose}
        className={`fixed inset-0 z-40 bg-slate-900/40 transition-opacity md:hidden ${
          open ? "opacity-100" : "pointer-events-none opacity-0"
        }`}
      />
      <div
        id="mobile-menu"
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label="Menu"
        inert={!open}
        className={`fixed inset-y-0 right-0 z-50 flex w-72 max-w-[85vw] flex-col bg-white shadow-xl transition-transform duration-200 md:hidden ${
          open ? "translate-x-0" : "translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-4">
          <span className="font-semibold text-slate-900">Menu</span>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            aria-label="Close menu"
            className="flex h-11 w-11 items-center justify-center rounded-md text-slate-600 hover:bg-slate-100"
          >
            <Icon icon={X} />
          </button>
        </div>
        {/* A distinct name from the desktop nav's "Primary" -- WCAG best
            practice gives every landmark of the same role a unique
            accessible name when more than one exists on the page, and
            this one is in the DOM (if not visible) at the same time as
            the desktop nav regardless of viewport. */}
        <nav aria-label="Primary (mobile)" className="flex-1 overflow-y-auto px-2 py-4">
          <ul className="space-y-1">
            {items.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.to === "/"}
                  onClick={onClose}
                  className={({ isActive }) =>
                    `flex min-h-[48px] items-center rounded-md px-3 text-base font-medium ${
                      isActive ? "bg-brand-50 text-brand-700" : "text-slate-700 hover:bg-slate-100"
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="border-t border-slate-200 p-4">
          <LinkButton
            href={appHref}
            variant="accent"
            className="w-full justify-center"
            onClick={onClose}
          >
            Get the App
          </LinkButton>
        </div>
      </div>
    </>
  );
}
