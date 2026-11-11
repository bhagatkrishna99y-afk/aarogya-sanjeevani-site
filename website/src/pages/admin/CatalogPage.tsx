import { useEffect, useState, type FormEvent } from "react";
import { supabase } from "../../lib/supabase";
import type { Tables } from "../../lib/database.types";
import { EmptyState, ErrorState, LoadingState, FRIENDLY_ERROR } from "../../components/StatusViews";

type Category = Tables<"service_categories">;
type Service = Tables<"services">;

export function CatalogPage() {
  const [categories, setCategories] = useState<Category[] | null>(null);
  const [services, setServices] = useState<Service[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setError(null);

    Promise.all([
      supabase.from("service_categories").select("*").order("sort_order"),
      supabase.from("services").select("*").order("sort_order"),
    ]).then(([catRes, svcRes]) => {
      if (!active) return;
      if (catRes.error || svcRes.error) {
        setError(FRIENDLY_ERROR);
        return;
      }
      setCategories(catRes.data ?? []);
      setServices(svcRes.data ?? []);
    });

    return () => {
      active = false;
    };
  }, [reloadKey]);

  if (error) return <ErrorState message={error} onRetry={() => setReloadKey((k) => k + 1)} />;
  if (categories === null || services === null) return <LoadingState label="Loading catalog…" />;

  return (
    <div className="space-y-10">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Catalog</h1>
        <p className="mt-1 text-sm text-slate-600">
          Manage the service categories and services patients see in the app.
        </p>
      </div>

      <CategoriesSection categories={categories} onChange={() => setReloadKey((k) => k + 1)} />
      <ServicesSection categories={categories} services={services} onChange={() => setReloadKey((k) => k + 1)} />
    </div>
  );
}

function CategoriesSection({ categories, onChange }: { categories: Category[]; onChange: () => void }) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (submitting || name.trim() === "") return;
    setSubmitting(true);
    setFormError(null);

    const { error } = await supabase.from("service_categories").insert({
      name: name.trim(),
      description: description.trim() || null,
      sort_order: categories.length,
    });

    setSubmitting(false);
    if (error) {
      setFormError("Couldn't add that category. It may already exist.");
      return;
    }
    setName("");
    setDescription("");
    onChange();
  }

  async function toggleActive(category: Category) {
    await supabase.from("service_categories").update({ is_active: !category.is_active }).eq("id", category.id);
    onChange();
  }

  return (
    <section>
      <h2 className="text-lg font-semibold text-slate-900">Categories</h2>

      {categories.length === 0 ? (
        <EmptyState title="No categories yet" />
      ) : (
        <ul className="mt-3 divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
          {categories.map((category) => (
            <li key={category.id} className="flex items-center justify-between gap-4 px-4 py-3">
              <div>
                <p className="font-medium text-slate-900">{category.name}</p>
                {category.description && <p className="text-sm text-slate-500">{category.description}</p>}
              </div>
              <button
                onClick={() => toggleActive(category)}
                className={`min-h-[48px] shrink-0 rounded-md px-3 text-sm font-medium ${
                  category.is_active ? "text-slate-600 hover:bg-slate-100" : "text-brand-700 hover:bg-brand-50"
                }`}
              >
                {category.is_active ? "Deactivate" : "Activate"}
              </button>
            </li>
          ))}
        </ul>
      )}

      <form onSubmit={handleSubmit} className="mt-4 flex flex-wrap items-end gap-3">
        <div className="flex-1">
          <label htmlFor="cat-name" className="text-sm font-medium text-slate-700">
            New category name
          </label>
          <input
            id="cat-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="mt-1 block min-h-[48px] w-full rounded-md border border-slate-300 px-3 text-sm"
          />
        </div>
        <div className="flex-1">
          <label htmlFor="cat-desc" className="text-sm font-medium text-slate-700">
            Description (optional)
          </label>
          <input
            id="cat-desc"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="mt-1 block min-h-[48px] w-full rounded-md border border-slate-300 px-3 text-sm"
          />
        </div>
        <button
          type="submit"
          disabled={submitting}
          className="min-h-[48px] rounded-md bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-60"
        >
          Add category
        </button>
      </form>
      {formError && <p className="mt-2 text-sm text-red-700">{formError}</p>}
    </section>
  );
}

function ServicesSection({
  categories,
  services,
  onChange,
}: {
  categories: Category[];
  services: Service[];
  onChange: () => void;
}) {
  const [name, setName] = useState("");
  const [categoryId, setCategoryId] = useState(categories[0]?.id ?? "");
  const [duration, setDuration] = useState(20);
  const [requiresDoctor, setRequiresDoctor] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (submitting || name.trim() === "" || categoryId === "") return;
    setSubmitting(true);
    setFormError(null);

    const { error } = await supabase.from("services").insert({
      name: name.trim(),
      category_id: categoryId,
      duration_minutes: duration,
      requires_doctor: requiresDoctor,
      sort_order: services.filter((s) => s.category_id === categoryId).length,
    });

    setSubmitting(false);
    if (error) {
      setFormError("Couldn't add that service. Please check the details and try again.");
      return;
    }
    setName("");
    onChange();
  }

  async function toggleActive(service: Service) {
    await supabase.from("services").update({ is_active: !service.is_active }).eq("id", service.id);
    onChange();
  }

  return (
    <section>
      <h2 className="text-lg font-semibold text-slate-900">Services</h2>

      {services.length === 0 ? (
        <EmptyState title="No services yet" />
      ) : (
        <ul className="mt-3 divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
          {services.map((service) => {
            const category = categories.find((c) => c.id === service.category_id);
            return (
              <li key={service.id} className="flex items-center justify-between gap-4 px-4 py-3">
                <div>
                  <p className="font-medium text-slate-900">{service.name}</p>
                  <p className="text-sm text-slate-500">
                    {category?.name ?? "Uncategorized"} · {service.duration_minutes} min ·{" "}
                    {service.requires_doctor ? "Doctor required" : "No doctor required"}
                  </p>
                </div>
                <button
                  onClick={() => toggleActive(service)}
                  className={`min-h-[48px] shrink-0 rounded-md px-3 text-sm font-medium ${
                    service.is_active ? "text-slate-600 hover:bg-slate-100" : "text-brand-700 hover:bg-brand-50"
                  }`}
                >
                  {service.is_active ? "Deactivate" : "Activate"}
                </button>
              </li>
            );
          })}
        </ul>
      )}

      {categories.length === 0 ? (
        <p className="mt-4 text-sm text-slate-500">Add a category first before adding services.</p>
      ) : (
        <form onSubmit={handleSubmit} className="mt-4 flex flex-wrap items-end gap-3">
          <div className="flex-1">
            <label htmlFor="svc-name" className="text-sm font-medium text-slate-700">
              New service name
            </label>
            <input
              id="svc-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="mt-1 block min-h-[48px] w-full rounded-md border border-slate-300 px-3 text-sm"
            />
          </div>
          <div>
            <label htmlFor="svc-category" className="text-sm font-medium text-slate-700">
              Category
            </label>
            <select
              id="svc-category"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              className="mt-1 block min-h-[48px] rounded-md border border-slate-300 px-3 text-sm"
            >
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="svc-duration" className="text-sm font-medium text-slate-700">
              Duration (min)
            </label>
            <input
              id="svc-duration"
              type="number"
              min={5}
              step={5}
              value={duration}
              onChange={(e) => setDuration(Number(e.target.value))}
              className="mt-1 block min-h-[48px] w-24 rounded-md border border-slate-300 px-3 text-sm"
            />
          </div>
          <label className="flex min-h-[48px] items-center gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={requiresDoctor}
              onChange={(e) => setRequiresDoctor(e.target.checked)}
              className="h-5 w-5"
            />
            Requires a doctor
          </label>
          <button
            type="submit"
            disabled={submitting}
            className="min-h-[48px] rounded-md bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-60"
          >
            Add service
          </button>
        </form>
      )}
      {formError && <p className="mt-2 text-sm text-red-700">{formError}</p>}
    </section>
  );
}
