import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { FaArrowLeft, FaArrowDown, FaArrowUp, FaPlus } from "react-icons/fa";
import * as categoryApi from "../../api/categoryApi";
import * as attributeApi from "../../api/adminCategoryAttributeApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { Modal } from "../../components/Modal/Modal";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import { toAttributeKey } from "../../utils/toAttributeKey";
import type { AttributeDataType, AttributeDefinitionResponse, CategoryResponse } from "../../types/api";
import "./AdminForm.css";
import "./AdminCategoryAttributesPage.css";

const DATA_TYPES: AttributeDataType[] = ["TEXT", "NUMBER", "DECIMAL", "BOOLEAN", "SELECT", "MULTI_SELECT"];
const OPTION_BACKED_TYPES: AttributeDataType[] = ["SELECT", "MULTI_SELECT"];

interface DefinitionFormState {
  name: string;
  key: string;
  dataType: AttributeDataType;
  required: boolean;
  filterable: boolean;
  unit: string;
  displayOrder: string;
  active: boolean;
}

const EMPTY_DEFINITION_FORM: DefinitionFormState = {
  name: "",
  key: "",
  dataType: "TEXT",
  required: false,
  filterable: false,
  unit: "",
  displayOrder: "0",
  active: true,
};

export function AdminCategoryAttributesPage() {
  const { id } = useParams<{ id: string }>();
  const { showToast } = useToast();
  const categoryId = Number(id);

  const [category, setCategory] = useState<CategoryResponse | null>(null);
  const [attributes, setAttributes] = useState<AttributeDefinitionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const [formOpen, setFormOpen] = useState(false);
  const [editingAttribute, setEditingAttribute] = useState<AttributeDefinitionResponse | null>(null);
  const [form, setForm] = useState<DefinitionFormState>(EMPTY_DEFINITION_FORM);
  const [keyTouched, setKeyTouched] = useState(false);
  const [newOptions, setNewOptions] = useState<{ value: string; label: string }[]>([{ value: "", label: "" }]);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [optionsAttribute, setOptionsAttribute] = useState<AttributeDefinitionResponse | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    return Promise.all([categoryApi.listCategories(), attributeApi.listAttributeDefinitions(categoryId)])
      .then(([categories, defs]) => {
        setCategory(categories.find((c) => c.id === categoryId) ?? null);
        setAttributes(defs);
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load category attributes.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [categoryId]);

  const openCreateForm = () => {
    setEditingAttribute(null);
    setForm(EMPTY_DEFINITION_FORM);
    setKeyTouched(false);
    setNewOptions([{ value: "", label: "" }]);
    setFormError(null);
    setFormOpen(true);
  };

  const openEditForm = (attribute: AttributeDefinitionResponse) => {
    setEditingAttribute(attribute);
    setForm({
      name: attribute.name,
      key: attribute.key,
      dataType: attribute.dataType,
      required: attribute.required,
      filterable: attribute.filterable,
      unit: attribute.unit ?? "",
      displayOrder: String(attribute.displayOrder),
      active: attribute.active,
    });
    setFormError(null);
    setFormOpen(true);
  };

  const handleNameChange = (value: string) => {
    setForm((f) => ({ ...f, name: value, key: keyTouched ? f.key : toAttributeKey(value) }));
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);

    const displayOrder = Number(form.displayOrder) || 0;
    if (!form.name.trim() || (!editingAttribute && !form.key.trim())) {
      setFormError("Name and key are required.");
      return;
    }

    const isOptionBacked = OPTION_BACKED_TYPES.includes(form.dataType);
    const options = newOptions
      .map((o) => ({ value: o.value.trim(), label: o.label.trim() }))
      .filter((o) => o.value && o.label);
    if (!editingAttribute && isOptionBacked && options.length === 0) {
      setFormError("SELECT and MULTI_SELECT attributes need at least one option.");
      return;
    }

    setSubmitting(true);
    try {
      if (editingAttribute) {
        const updated = await attributeApi.updateAttributeDefinition(categoryId, editingAttribute.id, {
          name: form.name.trim(),
          required: form.required,
          filterable: form.filterable,
          searchable: false,
          unit: form.unit.trim() || undefined,
          displayOrder,
          active: form.active,
        });
        setAttributes((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
        showToast("Attribute updated.");
      } else {
        const created = await attributeApi.createAttributeDefinition(categoryId, {
          key: form.key.trim(),
          name: form.name.trim(),
          dataType: form.dataType,
          required: form.required,
          filterable: form.filterable,
          searchable: false,
          unit: form.unit.trim() || undefined,
          displayOrder,
          options: isOptionBacked ? options.map((o, i) => ({ ...o, displayOrder: i + 1 })) : undefined,
        });
        setAttributes((prev) => [...prev, created]);
        showToast("Attribute created.");
      }
      setFormOpen(false);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not save this attribute."));
    } finally {
      setSubmitting(false);
    }
  };

  const toggleActive = async (attribute: AttributeDefinitionResponse) => {
    setBusyId(attribute.id);
    try {
      const updated = await attributeApi.setAttributeDefinitionActive(categoryId, attribute.id, !attribute.active);
      setAttributes((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
      showToast(updated.active ? "Attribute activated." : "Attribute deactivated.");
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not update this attribute."), "error");
    } finally {
      setBusyId(null);
    }
  };

  const move = async (attribute: AttributeDefinitionResponse, direction: "up" | "down") => {
    const sorted = [...attributes].sort((a, b) => a.displayOrder - b.displayOrder);
    const index = sorted.findIndex((a) => a.id === attribute.id);
    const swapIndex = direction === "up" ? index - 1 : index + 1;
    if (swapIndex < 0 || swapIndex >= sorted.length) return;
    const swapWith = sorted[swapIndex];

    setBusyId(attribute.id);
    try {
      const [updatedA, updatedB] = await Promise.all([
        attributeApi.updateAttributeDefinition(categoryId, attribute.id, {
          name: attribute.name,
          required: attribute.required,
          filterable: attribute.filterable,
          searchable: false,
          unit: attribute.unit ?? undefined,
          displayOrder: swapWith.displayOrder,
          active: attribute.active,
        }),
        attributeApi.updateAttributeDefinition(categoryId, swapWith.id, {
          name: swapWith.name,
          required: swapWith.required,
          filterable: swapWith.filterable,
          searchable: false,
          unit: swapWith.unit ?? undefined,
          displayOrder: attribute.displayOrder,
          active: swapWith.active,
        }),
      ]);
      setAttributes((prev) => prev.map((a) => (a.id === updatedA.id ? updatedA : a.id === updatedB.id ? updatedB : a)));
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not reorder attributes."), "error");
    } finally {
      setBusyId(null);
    }
  };

  const sortedAttributes = [...attributes].sort((a, b) => a.displayOrder - b.displayOrder);

  if (loading) return <LoadingState label="Loading category attributes…" />;
  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!category) return <ErrorState title="Category not found" message="This category could not be found." />;

  return (
    <div className="admin-category-attributes-page">
      <Link to="/admin/categories" className="admin-category-attributes-page__back">
        <FaArrowLeft aria-hidden="true" /> Back to Categories
      </Link>

      <AdminPageHeader
        title={`${category.name} Attributes`}
        subtitle="Define the structured fields customers fill in when posting an ad in this category."
        action={
          <button type="button" className="btn btn-primary" onClick={openCreateForm}>
            <FaPlus aria-hidden="true" /> New Attribute
          </button>
        }
      />

      {attributes.length === 0 && <EmptyState title="No attributes defined for this category yet." />}

      {attributes.length > 0 && (
        <div className="admin-category-attributes-page__table-wrap">
          <table className="admin-category-attributes-page__table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Key</th>
                <th>Type</th>
                <th>Required</th>
                <th>Filterable</th>
                <th>Unit</th>
                <th>Order</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {sortedAttributes.map((attribute, index) => (
                <tr key={attribute.id}>
                  <td className="admin-category-attributes-page__name">{attribute.name}</td>
                  <td className="admin-category-attributes-page__key">{attribute.key}</td>
                  <td>{attribute.dataType}</td>
                  <td>{attribute.required ? "Yes" : "No"}</td>
                  <td>{attribute.filterable ? "Yes" : "No"}</td>
                  <td>{attribute.unit ?? "—"}</td>
                  <td>
                    <div className="admin-category-attributes-page__reorder">
                      <button
                        type="button"
                        aria-label="Move up"
                        disabled={index === 0 || busyId === attribute.id}
                        onClick={() => move(attribute, "up")}
                      >
                        <FaArrowUp aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        aria-label="Move down"
                        disabled={index === sortedAttributes.length - 1 || busyId === attribute.id}
                        onClick={() => move(attribute, "down")}
                      >
                        <FaArrowDown aria-hidden="true" />
                      </button>
                    </div>
                  </td>
                  <td>
                    <span
                      className={`admin-category-attributes-page__status ${attribute.active ? "admin-category-attributes-page__status--active" : ""}`}
                    >
                      {attribute.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="admin-category-attributes-page__actions">
                    <button type="button" className="btn btn-secondary" onClick={() => openEditForm(attribute)}>
                      Edit
                    </button>
                    {OPTION_BACKED_TYPES.includes(attribute.dataType) && (
                      <button type="button" className="btn btn-secondary" onClick={() => setOptionsAttribute(attribute)}>
                        Options
                      </button>
                    )}
                    <button
                      type="button"
                      className="btn btn-outline"
                      disabled={busyId === attribute.id}
                      onClick={() => toggleActive(attribute)}
                    >
                      {attribute.active ? "Deactivate" : "Activate"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal open={formOpen} title={editingAttribute ? "Edit Attribute" : "New Attribute"} onClose={() => setFormOpen(false)}>
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          {formError && (
            <p className="admin-form__error" role="alert">
              {formError}
            </p>
          )}

          <div className="admin-form__field">
            <label htmlFor="attr-name">Name</label>
            <input id="attr-name" type="text" value={form.name} onChange={(e) => handleNameChange(e.target.value)} required />
          </div>

          <div className="admin-form__field">
            <label htmlFor="attr-key">Key</label>
            {editingAttribute ? (
              <input id="attr-key" type="text" value={form.key} disabled />
            ) : (
              <input
                id="attr-key"
                type="text"
                value={form.key}
                onChange={(e) => {
                  setKeyTouched(true);
                  setForm((f) => ({ ...f, key: e.target.value }));
                }}
                required
              />
            )}
            <p className="admin-form__hint">Machine-readable, camelCase. Cannot be changed once created.</p>
          </div>

          <div className="admin-form__field">
            <label htmlFor="attr-type">Data Type</label>
            {editingAttribute ? (
              <input id="attr-type" type="text" value={form.dataType} disabled />
            ) : (
              <select
                id="attr-type"
                value={form.dataType}
                onChange={(e) => setForm((f) => ({ ...f, dataType: e.target.value as AttributeDataType }))}
              >
                {DATA_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="admin-form__field">
            <label htmlFor="attr-required">
              <input
                id="attr-required"
                type="checkbox"
                checked={form.required}
                onChange={(e) => setForm((f) => ({ ...f, required: e.target.checked }))}
              />
              Required
            </label>
          </div>

          <div className="admin-form__field">
            <label htmlFor="attr-filterable">
              <input
                id="attr-filterable"
                type="checkbox"
                checked={form.filterable}
                onChange={(e) => setForm((f) => ({ ...f, filterable: e.target.checked }))}
              />
              Filterable
            </label>
          </div>

          <div className="admin-form__field">
            <label htmlFor="attr-unit">Unit (optional)</label>
            <input
              id="attr-unit"
              type="text"
              placeholder="e.g. km, perches, sq ft"
              value={form.unit}
              onChange={(e) => setForm((f) => ({ ...f, unit: e.target.value }))}
            />
          </div>

          <div className="admin-form__field">
            <label htmlFor="attr-order">Display Order</label>
            <input
              id="attr-order"
              type="number"
              value={form.displayOrder}
              onChange={(e) => setForm((f) => ({ ...f, displayOrder: e.target.value }))}
            />
          </div>

          {editingAttribute && (
            <div className="admin-form__field">
              <label htmlFor="attr-active">
                <input
                  id="attr-active"
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
                />
                Active
              </label>
            </div>
          )}

          {!editingAttribute && OPTION_BACKED_TYPES.includes(form.dataType) && (
            <div className="admin-form__field">
              <label>Options</label>
              {newOptions.map((option, index) => (
                <div className="admin-category-attributes-page__option-row" key={index}>
                  <input
                    type="text"
                    placeholder="Value (e.g. HYBRID)"
                    value={option.value}
                    onChange={(e) =>
                      setNewOptions((prev) => prev.map((o, i) => (i === index ? { ...o, value: e.target.value } : o)))
                    }
                  />
                  <input
                    type="text"
                    placeholder="Label (e.g. Hybrid)"
                    value={option.label}
                    onChange={(e) =>
                      setNewOptions((prev) => prev.map((o, i) => (i === index ? { ...o, label: e.target.value } : o)))
                    }
                  />
                </div>
              ))}
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setNewOptions((prev) => [...prev, { value: "", label: "" }])}
              >
                <FaPlus aria-hidden="true" /> Add option
              </button>
            </div>
          )}

          <div className="admin-form__actions">
            <button type="button" className="btn btn-secondary" onClick={() => setFormOpen(false)} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "Saving…" : editingAttribute ? "Save Changes" : "Create Attribute"}
            </button>
          </div>
        </form>
      </Modal>

      {optionsAttribute && (
        <AttributeOptionsModal
          categoryId={categoryId}
          attribute={optionsAttribute}
          onClose={() => setOptionsAttribute(null)}
          onOptionsChanged={(updated) => {
            setOptionsAttribute(updated);
            setAttributes((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
          }}
        />
      )}
    </div>
  );
}

interface AttributeOptionsModalProps {
  categoryId: number;
  attribute: AttributeDefinitionResponse;
  onClose: () => void;
  onOptionsChanged: (updated: AttributeDefinitionResponse) => void;
}

function AttributeOptionsModal({ categoryId, attribute, onClose, onOptionsChanged }: AttributeOptionsModalProps) {
  const { showToast } = useToast();
  const [newValue, setNewValue] = useState("");
  const [newLabel, setNewLabel] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = async () => {
    const defs = await attributeApi.listAttributeDefinitions(categoryId);
    const updated = defs.find((d) => d.id === attribute.id);
    if (updated) onOptionsChanged(updated);
  };

  const handleAddOption = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!newValue.trim() || !newLabel.trim()) {
      setError("Value and label are required.");
      return;
    }
    setSubmitting(true);
    try {
      await attributeApi.createAttributeOption(categoryId, attribute.id, {
        value: newValue.trim(),
        label: newLabel.trim(),
        displayOrder: attribute.options.length + 1,
      });
      setNewValue("");
      setNewLabel("");
      await refresh();
      showToast("Option added.");
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not add this option."));
    } finally {
      setSubmitting(false);
    }
  };

  const toggleOptionActive = async (optionId: number, active: boolean) => {
    try {
      await attributeApi.setAttributeOptionActive(categoryId, attribute.id, optionId, active);
      await refresh();
      showToast(active ? "Option activated." : "Option deactivated.");
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not update this option."), "error");
    }
  };

  const sortedOptions = [...attribute.options].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <Modal open title={`Options for ${attribute.name}`} onClose={onClose}>
      <div className="admin-category-attributes-page__options-list">
        {sortedOptions.length === 0 && <p className="admin-form__hint">No options yet.</p>}
        {sortedOptions.map((option) => (
          <div className="admin-category-attributes-page__option-item" key={option.id}>
            <div>
              <span className="admin-category-attributes-page__name">{option.label}</span>
              <span className="admin-category-attributes-page__key"> ({option.value})</span>
            </div>
            <button type="button" className="btn btn-outline" onClick={() => toggleOptionActive(option.id, !option.active)}>
              {option.active ? "Deactivate" : "Activate"}
            </button>
          </div>
        ))}
      </div>

      <form className="admin-form" onSubmit={handleAddOption} noValidate>
        {error && (
          <p className="admin-form__error" role="alert">
            {error}
          </p>
        )}
        <div className="admin-category-attributes-page__option-row">
          <input type="text" placeholder="Value (e.g. HYBRID)" value={newValue} onChange={(e) => setNewValue(e.target.value)} />
          <input type="text" placeholder="Label (e.g. Hybrid)" value={newLabel} onChange={(e) => setNewLabel(e.target.value)} />
        </div>
        <div className="admin-form__actions">
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? "Adding…" : "Add Option"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
