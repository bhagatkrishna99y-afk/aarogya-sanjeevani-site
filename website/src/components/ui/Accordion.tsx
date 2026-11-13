import {
  createContext,
  useContext,
  useEffect,
  useId,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { ChevronDown } from "lucide-react";
import { Icon } from "./Icon";

interface AccordionContextValue {
  isOpen: (id: string) => boolean;
  toggle: (id: string) => void;
}

const AccordionContext = createContext<AccordionContextValue | null>(null);

export interface AccordionProps {
  children: ReactNode;
  /** Allow more than one item open at once. Default: false (classic single-open FAQ behavior). */
  allowMultiple?: boolean;
  defaultOpenIds?: string[];
  className?: string;
  /** Accessible label for the group, e.g. "Frequently asked questions". */
  "aria-label"?: string;
}

/** An accessible accordion (FAQ pattern): each header is a real `<button>` with `aria-expanded`/`aria-controls`, and Up/Down/Home/End move focus between headers per the WAI-ARIA accordion pattern. */
export function Accordion({
  children,
  allowMultiple = false,
  defaultOpenIds = [],
  className,
  ...props
}: AccordionProps) {
  const [openIds, setOpenIds] = useState<Set<string>>(new Set(defaultOpenIds));
  const containerRef = useRef<HTMLDivElement>(null);

  function toggle(id: string) {
    setOpenIds((prev) => {
      const next = allowMultiple ? new Set(prev) : new Set<string>();
      if (prev.has(id) && (allowMultiple || prev.size === 1)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  // Attached imperatively (not as a JSX onKeyDown) because this container
  // is a pure event-delegation wrapper, not an interactive element itself
  // -- the keyboard behavior it provides (Up/Down/Home/End moving focus
  // between header buttons) belongs to those already-focusable, already
  // keyboard-operable <button> elements, not to this div.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    function handleKeyDown(event: KeyboardEvent) {
      const headers = Array.from(
        container!.querySelectorAll<HTMLButtonElement>("[data-accordion-header]"),
      );
      const currentIndex = headers.indexOf(document.activeElement as HTMLButtonElement);
      if (currentIndex === -1) return;

      let targetIndex: number | null = null;
      if (event.key === "ArrowDown") targetIndex = (currentIndex + 1) % headers.length;
      else if (event.key === "ArrowUp")
        targetIndex = (currentIndex - 1 + headers.length) % headers.length;
      else if (event.key === "Home") targetIndex = 0;
      else if (event.key === "End") targetIndex = headers.length - 1;

      if (targetIndex !== null) {
        event.preventDefault();
        headers[targetIndex]?.focus();
      }
    }

    container.addEventListener("keydown", handleKeyDown);
    return () => container.removeEventListener("keydown", handleKeyDown);
  }, []);

  return (
    <AccordionContext.Provider value={{ isOpen: (id) => openIds.has(id), toggle }}>
      <div ref={containerRef} className={className} {...props}>
        {children}
      </div>
    </AccordionContext.Provider>
  );
}

export interface AccordionItemProps {
  id: string;
  question: ReactNode;
  children: ReactNode;
  className?: string;
}

export function AccordionItem({ id, question, children, className }: AccordionItemProps) {
  const context = useContext(AccordionContext);
  if (!context) {
    throw new Error("AccordionItem must be rendered inside an Accordion");
  }
  const { isOpen, toggle } = context;
  const open = isOpen(id);
  const panelId = useId();
  const headerId = useId();

  return (
    <div className={["border-b border-slate-200", className].filter(Boolean).join(" ")}>
      <button
        type="button"
        id={headerId}
        data-accordion-header
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => toggle(id)}
        className="flex min-h-[48px] w-full items-center justify-between gap-4 py-4 text-left text-sm font-medium text-slate-900 hover:text-brand-700"
      >
        {question}
        <Icon
          icon={ChevronDown}
          className={`shrink-0 text-slate-500 transition-transform ${open ? "rotate-180" : ""}`}
        />
      </button>
      <div
        id={panelId}
        role="region"
        aria-labelledby={headerId}
        hidden={!open}
        className="pb-4 text-sm text-slate-600"
      >
        {children}
      </div>
    </div>
  );
}
