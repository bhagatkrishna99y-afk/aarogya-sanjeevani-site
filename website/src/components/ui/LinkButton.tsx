import { forwardRef, type ReactNode } from "react";
import { Link, type LinkProps } from "react-router-dom";
import { buttonClasses, type ButtonSize, type ButtonVariant } from "./button-styles";

type InternalLinkButtonProps = { to: LinkProps["to"]; href?: never; children: ReactNode } & Omit<
  LinkProps,
  "to" | "className" | "children"
>;
type ExternalLinkButtonProps = { href: string; to?: never; children: ReactNode } & Omit<
  React.AnchorHTMLAttributes<HTMLAnchorElement>,
  "href" | "className" | "children"
>;

export type LinkButtonProps = (InternalLinkButtonProps | ExternalLinkButtonProps) & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  className?: string;
};

/**
 * A link that looks exactly like Button -- same variants/sizes, same
 * verified-AA color pairs. Pass `to` for an internal React Router route,
 * or `href` for a plain external/absolute link (gets
 * `target="_blank" rel="noopener noreferrer"` automatically unless you
 * override `target` yourself).
 */
export const LinkButton = forwardRef<HTMLAnchorElement, LinkButtonProps>(function LinkButton(
  { variant = "primary", size = "md", className, ...props },
  ref,
) {
  const classes = buttonClasses(variant, size, className);

  if ("to" in props && props.to !== undefined) {
    const { to, children, ...rest } = props;
    return (
      <Link ref={ref} to={to} className={classes} {...rest}>
        {children}
      </Link>
    );
  }

  const { href, target, children, ...rest } = props as ExternalLinkButtonProps;
  return (
    <a
      ref={ref}
      href={href}
      target={target ?? "_blank"}
      rel={target ? undefined : "noopener noreferrer"}
      className={classes}
      {...rest}
    >
      {children}
    </a>
  );
});
