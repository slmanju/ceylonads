import { useEffect, useState, type FormEvent } from "react";
import { FaPlus } from "react-icons/fa";
import * as locationApi from "../../api/locationApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { LocationTree } from "../../components/LocationTree/LocationTree";
import { Modal } from "../../components/Modal/Modal";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import { slugify } from "../../utils/slugify";
import type { LocationResponse, LocationType } from "../../types/api";
import "./AdminForm.css";
import "./AdminLocationsPage.css";

const TYPES: LocationType[] = ["PROVINCE", "DISTRICT", "CITY"];

const PARENT_TYPE: Record<LocationType, LocationType | null> = {
  PROVINCE: null,
  DISTRICT: "PROVINCE",
  CITY: "DISTRICT",
};

export function AdminLocationsPage() {
  const { showToast } = useToast();
  const [locations, setLocations] = useState<LocationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);

  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugTouched, setSlugTouched] = useState(false);
  const [type, setType] = useState<LocationType>("CITY");
  const [parentSlug, setParentSlug] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const load = () => {
    setLoading(true);
    setError(null);
    return locationApi
      .listLocations()
      .then(setLocations)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load locations.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openForm = () => {
    setName("");
    setSlug("");
    setSlugTouched(false);
    setType("CITY");
    setParentSlug("");
    setFormError(null);
    setFormOpen(true);
  };

  const handleNameChange = (value: string) => {
    setName(value);
    if (!slugTouched) setSlug(slugify(value));
  };

  const requiredParentType = PARENT_TYPE[type];
  const parentOptions = requiredParentType
    ? locations.filter((l) => l.type === requiredParentType).sort((a, b) => a.name.localeCompare(b.name))
    : [];

  const handleTypeChange = (value: LocationType) => {
    setType(value);
    setParentSlug("");
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);

    if (!name.trim() || !slug.trim()) {
      setFormError("Name and slug are required.");
      return;
    }
    if (requiredParentType && !parentSlug) {
      setFormError(`Select the parent ${requiredParentType.toLowerCase()}.`);
      return;
    }

    setSubmitting(true);
    try {
      await locationApi.createLocation({
        name: name.trim(),
        slug: slug.trim(),
        type,
        parentSlug: parentSlug || undefined,
      });
      showToast("Location created.");
      setFormOpen(false);
      load();
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not create this location."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="admin-locations-page">
      <AdminPageHeader
        title="Locations"
        subtitle="Manage the Sri Lankan location hierarchy."
        action={
          <button type="button" className="btn btn-primary" onClick={openForm}>
            <FaPlus aria-hidden="true" /> New Location
          </button>
        }
      />

      {loading && <LoadingState label="Loading locations…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && locations.length === 0 && <EmptyState title="No locations found." />}

      {!loading && !error && locations.length > 0 && (
        <div className="admin-locations-page__panel">
          <LocationTree locations={locations} />
        </div>
      )}

      <Modal open={formOpen} title="New Location" onClose={() => setFormOpen(false)}>
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          {formError && (
            <p className="admin-form__error" role="alert">
              {formError}
            </p>
          )}

          <div className="admin-form__field">
            <label htmlFor="location-type">Type</label>
            <select id="location-type" value={type} onChange={(e) => handleTypeChange(e.target.value as LocationType)}>
              {TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>

          <div className="admin-form__field">
            <label htmlFor="location-name">Name</label>
            <input
              id="location-name"
              type="text"
              value={name}
              onChange={(e) => handleNameChange(e.target.value)}
              required
            />
          </div>

          <div className="admin-form__field">
            <label htmlFor="location-slug">Slug</label>
            <input
              id="location-slug"
              type="text"
              value={slug}
              onChange={(e) => {
                setSlugTouched(true);
                setSlug(e.target.value);
              }}
              required
            />
          </div>

          {requiredParentType && (
            <div className="admin-form__field">
              <label htmlFor="location-parent">Parent {requiredParentType}</label>
              <select id="location-parent" value={parentSlug} onChange={(e) => setParentSlug(e.target.value)}>
                <option value="">Select a {requiredParentType.toLowerCase()}…</option>
                {parentOptions.map((l) => (
                  <option key={l.id} value={l.slug}>
                    {l.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="admin-form__actions">
            <button type="button" className="btn btn-secondary" onClick={() => setFormOpen(false)} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "Creating…" : "Create Location"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
