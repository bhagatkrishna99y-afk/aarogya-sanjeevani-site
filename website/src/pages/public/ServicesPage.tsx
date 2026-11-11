import { useEffect, useState } from "react";
import { supabase } from "../../lib/supabase";
import type { Tables } from "../../lib/database.types";
import { EmptyState, ErrorState, LoadingState, FRIENDLY_ERROR } from "../../components/StatusViews";

type Category = Tables<"service_categories"> & { services: Tables<"services">[] };

export function ServicesPage() {
  const [categories, setCategories] = useState<Category[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setCategories(null);
    setError(null);

    supabase
      .from("service_categories")
      .select("*, services(*)")
      .eq("is_active", true)
      .order("sort_order")
      .then(({ data, error: fetchError }) => {
        if (!active) return;
        if (fetchError) {
          setError(FRIENDLY_ERROR);
          return;
        }
        setCategories((data ?? []) as Category[]);
      });

    return () => {
      active = false;
    };
  }, [reloadKey]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold text-slate-900">Services</h1>
      <p className="mt-2 max-w-2xl text-slate-600">
        Everything listed here can be booked from the Patient app. Services marked "no doctor
        required" are walk-in or lab-style services.
      </p>

      <div className="mt-8">
        {error && <ErrorState message={error} onRetry={() => setReloadKey((k) => k + 1)} />}
        {!error && categories === null && <LoadingState label="Loading services…" />}
        {!error && categories !== null && categories.length === 0 && (
          <EmptyState title="No services are available right now" description="Please check back soon." />
        )}
        {!error && categories !== null && categories.length > 0 && (
          <div className="space-y-10">
            {categories.map((category) => {
              const activeServices = category.services.filter((s) => s.is_active);
              if (activeServices.length === 0) return null;
              return (
                <section key={category.id}>
                  <h2 className="text-xl font-semibold text-slate-900">{category.name}</h2>
                  {category.description && (
                    <p className="mt-1 text-sm text-slate-600">{category.description}</p>
                  )}
                  <ul className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {activeServices.map((service) => (
                      <li
                        key={service.id}
                        className="rounded-lg border border-slate-200 bg-white p-4"
                      >
                        <p className="font-medium text-slate-900">{service.name}</p>
                        {service.description && (
                          <p className="mt-1 text-sm text-slate-600">{service.description}</p>
                        )}
                        <div className="mt-3 flex items-center gap-2 text-xs text-slate-500">
                          <span className="rounded-full bg-slate-100 px-2 py-1">
                            {service.duration_minutes} min
                          </span>
                          <span className="rounded-full bg-slate-100 px-2 py-1">
                            {service.requires_doctor ? "Doctor required" : "No doctor required"}
                          </span>
                        </div>
                      </li>
                    ))}
                  </ul>
                </section>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
