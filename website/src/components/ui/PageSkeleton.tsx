import { Container } from "./Container";
import { Skeleton } from "./Skeleton";

/**
 * Generic page-shaped loading placeholder -- the `<Suspense fallback>`
 * for every lazy-loaded route (src/app/router.tsx). A rough approximation
 * of "a heading, some body copy, a row of cards" rather than a
 * page-specific skeleton, since it has to cover every route, not just
 * one; `aria-hidden` (purely decorative, like `Skeleton` itself) since
 * there's nothing here a screen reader needs announced mid-navigation.
 */
export function PageSkeleton() {
  return (
    <div aria-hidden="true" className="py-16 sm:py-24">
      <Container>
        <Skeleton className="h-10 w-2/3 max-w-md" />
        <Skeleton className="mt-4 h-4 w-full max-w-xl" />
        <Skeleton className="mt-2 h-4 w-5/6 max-w-xl" />
        <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
        </div>
      </Container>
    </div>
  );
}
