import { useEffect, useRef, useState } from "react";
import { NavLink } from "react-router-dom";
import { Menu } from "lucide-react";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { siteContent } from "@/content/site";
import logo from "@/assets/logo.png";
import { MobileMenu } from "./MobileMenu";

/**
 * Keeps `--header-height` (consumed by `html { scroll-padding-top }` in
 * src/styles/index.css) in sync with the header's real rendered height,
 * so the sticky header never ends up covering an in-page anchor target
 * (or the skip link's own target) after a hash/anchor jump. Measured
 * rather than hardcoded because the header's height legitimately differs
 * between the mobile and desktop layouts.
 */
function useHeaderHeightVar(ref: React.RefObject<HTMLElement | null>) {
  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const setVar = () =>
      document.documentElement.style.setProperty("--header-height", `${el.offsetHeight}px`);
    setVar();

    const observer = new ResizeObserver(setVar);
    observer.observe(el);
    return () => observer.disconnect();
  }, [ref]);
}

export function Header() {
  const [menuOpen, setMenuOpen] = useState(false);
  const headerRef = useRef<HTMLElement>(null);
  const hamburgerRef = useRef<HTMLButtonElement>(null);

  useHeaderHeightVar(headerRef);

  return (
    <header ref={headerRef} className="sticky top-0 z-30 border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
        <NavLink to="/" className="flex items-center">
          <img src={logo} alt={siteContent.name} className="h-16 w-auto py-1" />
        </NavLink>

        <nav aria-label="Primary" className="hidden md:block">
          <ul className="flex items-center gap-1">
            {siteContent.nav.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.to === "/"}
                  className={({ isActive }) =>
                    `inline-flex min-h-[48px] items-center rounded-md px-3 text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-brand-50 text-brand-700"
                        : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="flex items-center gap-2">
          <LinkButton
            href={siteContent.playStore.patient}
            variant="accent"
            size="sm"
            className="hidden sm:inline-flex"
          >
            Get the App
          </LinkButton>
          <button
            ref={hamburgerRef}
            type="button"
            onClick={() => setMenuOpen(true)}
            aria-expanded={menuOpen}
            aria-controls="mobile-menu"
            aria-label="Open menu"
            className="flex h-11 w-11 items-center justify-center rounded-md text-slate-700 hover:bg-slate-100 md:hidden"
          >
            <Icon icon={Menu} />
          </button>
        </div>
      </div>

      <MobileMenu
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        items={siteContent.nav}
        appHref={siteContent.playStore.patient}
        returnFocusRef={hamburgerRef}
      />
    </header>
  );
}
