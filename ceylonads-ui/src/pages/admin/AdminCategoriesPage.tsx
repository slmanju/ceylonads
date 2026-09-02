import { useEffect, useState, type FormEvent } from "react";
import { FaPlus } from "react-icons/fa";
import * as categoryApi from "../../api/categoryApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { CategoryTree } from "../../components/CategoryTree/CategoryTree";
import { Modal } from "../../components/Modal/Modal";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import { slugify } from "../../utils/slugify";
import type { CategoryResponse } from "../../types/api";
import "./AdminForm.css";
import "./AdminCategoriesPage.css";

export function AdminCategoriesPage() {
  const { showToast } = useToast();
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);

  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugTouched, setSlugTouched] = useState(false);
  const [parentSlug, setParentSlug] = useState("");
  const [displayOrder, setDisplayOrder] = useState(0);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const load = () => {
    setLoading(true);
    setError(null);
    return categoryApi
      .listCategories()
      .then(setCategories)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load categories.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openForm = () => {
    setName("");
    setSlug("");
    setSlugTouched(false);
    setParentSlug("");
    setDisplayOrder(0);
    setFormError(null);
    setFormOpen(true);
  };

  const handleNameChange = (value: string) => {
    setName(value);
    if (!slugTouched) setSlug(slugify(value));
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);

    if (!name.trim() || !slug.trim()) {
      setFormError("Name and slug are required.");
      return;
    }

    setSubmitting(true);
    try {
      await categoryApi.createCategory({
        name: name.trim(),
        slug: slug.trim(),
        parentSlug: parentSlug || undefined,
        displayOrder,
      });
      showToast("Category created.");
      setFormOpen(false);
      load();
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not create this category."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="admin-categories-page">
      <AdminPageHeader
        title="Categories"
        subtitle="Manage the marketplace category hierarchy."
        action={
          <button type="button" className="btn btn-primary" onClick={openForm}>
            <FaPlus aria-hidden="true" /> New Category
          </button>
        }
      />

      {loading && <LoadingState label="Loading categories…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && categories.length === 0 && <EmptyState title="No categories found." />}

      {!loading && !error && categories.length > 0 && (
        <div className="admin-categories-page__panel">
          <CategoryTree categories={categories} />
        </div>
      )}

      <Modal open={formOpen} title="New Category" onClose={() => setFormOpen(false)}>
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          {formError && (
            <p className="admin-form__error" role="alert">
              {formError}
            </p>
          )}

          <div className="admin-form__field">
            <label htmlFor="category-name">Name</label>
            <input
              id="category-name"
              type="text"
              value={name}
              onChange={(e) => handleNameChange(e.target.value)}
              required
            />
          </div>

          <div className="admin-form__field">
            <label htmlFor="category-slug">Slug</label>
            <input
              id="category-slug"
              type="text"
              value={slug}
              onChange={(e) => {
                setSlugTouched(true);
                setSlug(e.target.value);
              }}
              required
            />
          </div>

          <div className="admin-form__field">
            <label htmlFor="category-parent">Parent Category</label>
            <select id="category-parent" value={parentSlug} onChange={(e) => setParentSlug(e.target.value)}>
              <option value="">None (top level)</option>
              {categories
                .slice()
                .sort((a, b) => a.name.localeCompare(b.name))
                .map((c) => (
                  <option key={c.id} value={c.slug}>
                    {c.name}
                  </option>
                ))}
            </select>
          </div>

          <div className="admin-form__field">
            <label htmlFor="category-order">Display Order</label>
            <input
              id="category-order"
              type="number"
              value={displayOrder}
              onChange={(e) => setDisplayOrder(Number(e.target.value))}
            />
          </div>

          <div className="admin-form__actions">
            <button type="button" className="btn btn-secondary" onClick={() => setFormOpen(false)} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "Creating…" : "Create Category"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
