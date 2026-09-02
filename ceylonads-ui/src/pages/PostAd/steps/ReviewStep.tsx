import { FaEdit, FaExclamationTriangle } from "react-icons/fa";
import type { AttributeDefinitionResponse, LocationResponse, MediaResponse } from "../../../types/api";
import { resolveMediaUrl } from "../../../api/apiClient";
import { formatAdPrice } from "../../../utils/formatPrice";
import type { AdFormValues, PendingPhoto } from "../types";

interface ReviewStepProps {
  values: AdFormValues;
  attributeDefinitions: AttributeDefinitionResponse[];
  locations: LocationResponse[];
  existingMedia: MediaResponse[];
  pendingPhotos: PendingPhoto[];
  mode: "create" | "edit";
  submitting: boolean;
  submitError: string | null;
  onEditStep: (index: number) => void;
  onSubmit: () => void;
}

function formatAttributeDisplayValue(definition: AttributeDefinitionResponse, raw: string): string {
  switch (definition.dataType) {
    case "BOOLEAN":
      return raw === "true" ? "Yes" : "No";
    case "SELECT":
    case "MULTI_SELECT": {
      const labels = raw
        .split(",")
        .map((v) => definition.options.find((o) => o.value === v)?.label ?? v)
        .filter(Boolean);
      return labels.join(", ");
    }
    default:
      return definition.unit ? `${raw} ${definition.unit}` : raw;
  }
}

export function ReviewStep({
  values,
  attributeDefinitions,
  locations,
  existingMedia,
  pendingPhotos,
  mode,
  submitting,
  submitError,
  onEditStep,
  onSubmit,
}: ReviewStepProps) {
  const priceNumber = Number(values.price) || 0;
  const photoCount = existingMedia.length + pendingPhotos.length;
  const failedPhotos = pendingPhotos.filter((p) => p.status === "error");
  const filledAttributes = [...attributeDefinitions]
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .filter((definition) => (values.attributes[definition.key] ?? "").trim() !== "");
  const selectedLocationNames = values.locationSlugs
    .map((slug) => locations.find((l) => l.slug === slug)?.name)
    .filter((name): name is string => Boolean(name));

  const contactName = values.contactName.trim();
  const phoneNumber = values.phoneNumber.trim();
  const whatsappNumber = (values.whatsappSameAsPhone ? values.phoneNumber : values.whatsappNumber).trim();
  const hasContactOverride = contactName !== "" || phoneNumber !== "" || whatsappNumber !== "";

  return (
    <div className="post-ad-step">
      <h2 className="post-ad-step__title">Review your ad</h2>
      <p className="post-ad-step__subtitle">Make sure everything looks right before submitting.</p>

      <div className="review-step">
        <div className="review-step__row">
          <div>
            <p className="review-step__label">Category</p>
            <p className="review-step__value">{values.categoryPath}</p>
          </div>
          <button type="button" className="review-step__edit" onClick={() => onEditStep(0)}>
            <FaEdit aria-hidden="true" /> Edit
          </button>
        </div>

        <div className="review-step__row">
          <div>
            <p className="review-step__label">Title</p>
            <p className="review-step__value">{values.title}</p>
            <p className="review-step__label review-step__label--spaced">Price</p>
            <p className="review-step__value review-step__value--price">{formatAdPrice(priceNumber)}</p>
            {values.description.trim() !== "" && (
              <>
                <p className="review-step__label review-step__label--spaced">Description</p>
                <p className="review-step__value review-step__description">{values.description}</p>
              </>
            )}
          </div>
          <button type="button" className="review-step__edit" onClick={() => onEditStep(1)}>
            <FaEdit aria-hidden="true" /> Edit
          </button>
        </div>

        {selectedLocationNames.length > 0 && (
          <div className="review-step__row">
            <div>
              <p className="review-step__label">Location</p>
              <p className="review-step__value">{selectedLocationNames.join(", ")}</p>
            </div>
            <button type="button" className="review-step__edit" onClick={() => onEditStep(2)}>
              <FaEdit aria-hidden="true" /> Edit
            </button>
          </div>
        )}

        {filledAttributes.length > 0 && (
          <div className="review-step__row">
            <div>
              <p className="review-step__label">{values.categoryPath} Details</p>
              {filledAttributes.map((definition) => (
                <p className="review-step__value" key={definition.key}>
                  {definition.name}: {formatAttributeDisplayValue(definition, values.attributes[definition.key])}
                </p>
              ))}
            </div>
            <button type="button" className="review-step__edit" onClick={() => onEditStep(1)}>
              <FaEdit aria-hidden="true" /> Edit
            </button>
          </div>
        )}

        <div className="review-step__row">
          <div>
            <p className="review-step__label">Contact</p>
            {hasContactOverride ? (
              <>
                {contactName !== "" && <p className="review-step__value">{contactName}</p>}
                {phoneNumber !== "" && <p className="review-step__value">{phoneNumber}</p>}
                {whatsappNumber !== "" && (
                  <p className="review-step__value">
                    WhatsApp: {whatsappNumber === phoneNumber ? "same number" : whatsappNumber}
                  </p>
                )}
              </>
            ) : (
              <p className="review-step__value">Using account contact details</p>
            )}
          </div>
          <button type="button" className="review-step__edit" onClick={() => onEditStep(1)}>
            <FaEdit aria-hidden="true" /> Edit
          </button>
        </div>

        <div className="review-step__row">
          <div className="review-step__photos">
            <p className="review-step__label">Photos ({photoCount})</p>
            <div className="review-step__photo-strip">
              {existingMedia.map((m) => (
                <img key={m.id} src={resolveMediaUrl(m.url)} alt="" />
              ))}
              {pendingPhotos.map((p) => (
                <img key={p.localId} src={p.previewUrl} alt="" style={{ opacity: p.status === "error" ? 0.4 : 1 }} />
              ))}
            </div>
          </div>
          <button type="button" className="review-step__edit" onClick={() => onEditStep(3)}>
            <FaEdit aria-hidden="true" /> Edit
          </button>
        </div>
      </div>

      {failedPhotos.length > 0 && (
        <p className="post-ad-form__error">
          <FaExclamationTriangle aria-hidden="true" /> {failedPhotos.length} photo(s) failed to upload. You can retry
          from the Photos step.
        </p>
      )}

      {submitError && <p className="post-ad-form__error">{submitError}</p>}

      <button type="button" className="btn btn-primary btn-block review-step__submit" onClick={onSubmit} disabled={submitting}>
        {submitting ? "Submitting…" : mode === "edit" ? "Save Changes" : "Submit Ad"}
      </button>
    </div>
  );
}
