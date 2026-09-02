import { formatAdPrice } from "../../../utils/formatPrice";
import { DynamicAttributeForm } from "../../../components/AttributeFields/DynamicAttributeForm";
import type { AttributeDefinitionResponse } from "../../../types/api";
import type { DetailsFieldErrors } from "../types";

type DetailsField = "title" | "description" | "price" | "contactName" | "phoneNumber" | "whatsappNumber";

interface DetailsStepProps {
  title: string;
  description: string;
  price: string;
  categoryPath: string;
  errors: DetailsFieldErrors;
  onChange: (field: DetailsField, value: string) => void;
  attributeDefinitions: AttributeDefinitionResponse[];
  attributeValues: Record<string, string>;
  attributeErrors: Record<string, string>;
  onAttributeChange: (key: string, value: string) => void;
  contactName: string;
  phoneNumber: string;
  whatsappNumber: string;
  whatsappSameAsPhone: boolean;
  onWhatsappSameAsPhoneChange: (checked: boolean) => void;
  // The account's own contact, shown as a "using account X" hint under each blank field so the
  // seller can see what buyers will actually see without it being silently copied into the ad.
  accountDisplayName?: string | null;
  accountPhone?: string | null;
}

export function DetailsStep({
  title,
  description,
  price,
  categoryPath,
  errors,
  onChange,
  attributeDefinitions,
  attributeValues,
  attributeErrors,
  onAttributeChange,
  contactName,
  phoneNumber,
  whatsappNumber,
  whatsappSameAsPhone,
  onWhatsappSameAsPhoneChange,
  accountDisplayName,
  accountPhone,
}: DetailsStepProps) {
  const priceNumber = Number(price);
  const showPricePreview = price.trim() !== "" && !Number.isNaN(priceNumber);

  return (
    <div className="post-ad-step">
      <h2 className="post-ad-step__title">Tell us about your ad</h2>
      <p className="post-ad-step__subtitle">{categoryPath ? `Category: ${categoryPath}` : "Add the details buyers need to know."}</p>

      <div className="post-ad-form">
        <div className="post-ad-form__field">
          <label htmlFor="ad-title">Title</label>
          <input
            id="ad-title"
            type="text"
            placeholder="e.g. Toyota Aqua 2019"
            value={title}
            maxLength={180}
            onChange={(e) => onChange("title", e.target.value)}
          />
          <div className="post-ad-form__hint-row">
            {errors.title && <span className="post-ad-form__error">{errors.title}</span>}
            <span className="post-ad-form__counter">{title.length}/180</span>
          </div>
        </div>

        <div className="post-ad-form__field">
          <label htmlFor="ad-price">Price (Rs.) (optional)</label>
          <input
            id="ad-price"
            type="number"
            inputMode="decimal"
            min={0}
            placeholder="e.g. 8950000"
            value={price}
            onChange={(e) => onChange("price", e.target.value)}
          />
          <div className="post-ad-form__hint-row">
            {errors.price ? (
              <span className="post-ad-form__error">{errors.price}</span>
            ) : showPricePreview ? (
              <span className="post-ad-form__hint">{formatAdPrice(priceNumber)}</span>
            ) : (
              <span className="post-ad-form__hint">Leave blank if you prefer buyers to contact you for the price.</span>
            )}
          </div>
        </div>

        <div className="post-ad-form__field">
          <label htmlFor="ad-description">Description (optional)</label>
          <textarea
            id="ad-description"
            rows={6}
            placeholder="Describe your item or service — condition, features, why it's a great deal…"
            value={description}
            maxLength={5000}
            onChange={(e) => onChange("description", e.target.value)}
          />
          <div className="post-ad-form__hint-row">
            {errors.description && <span className="post-ad-form__error">{errors.description}</span>}
            <span className="post-ad-form__counter">{description.length}/5000</span>
          </div>
        </div>

        {attributeDefinitions.length > 0 && (
          <DynamicAttributeForm
            definitions={attributeDefinitions}
            mode="form"
            values={attributeValues}
            errors={attributeErrors}
            onChange={onAttributeChange}
          />
        )}

        <div className="post-ad-contact">
          <h3 className="post-ad-contact__title">Contact details</h3>
          <p className="post-ad-contact__hint">Leave blank to use your account contact details.</p>

          <div className="post-ad-form__field">
            <label htmlFor="ad-phone">Phone number (optional)</label>
            <input
              id="ad-phone"
              type="tel"
              placeholder="07XXXXXXXX"
              value={phoneNumber}
              onChange={(e) => onChange("phoneNumber", e.target.value)}
            />
            <div className="post-ad-form__hint-row">
              {errors.phoneNumber ? (
                <span className="post-ad-form__error">{errors.phoneNumber}</span>
              ) : (
                phoneNumber.trim() === "" &&
                accountPhone && <span className="post-ad-form__hint">Using account number: {accountPhone}</span>
              )}
            </div>
          </div>

          <div className="post-ad-contact__checkbox">
            <label htmlFor="ad-whatsapp-same">
              <input
                id="ad-whatsapp-same"
                type="checkbox"
                checked={whatsappSameAsPhone}
                onChange={(e) => onWhatsappSameAsPhoneChange(e.target.checked)}
              />
              Use same number for WhatsApp
            </label>
          </div>

          {!whatsappSameAsPhone && (
            <div className="post-ad-form__field">
              <label htmlFor="ad-whatsapp">WhatsApp number (optional)</label>
              <input
                id="ad-whatsapp"
                type="tel"
                placeholder="07XXXXXXXX"
                value={whatsappNumber}
                onChange={(e) => onChange("whatsappNumber", e.target.value)}
              />
              <div className="post-ad-form__hint-row">
                {errors.whatsappNumber ? (
                  <span className="post-ad-form__error">{errors.whatsappNumber}</span>
                ) : (
                  whatsappNumber.trim() === "" &&
                  accountPhone && <span className="post-ad-form__hint">Using account number: {accountPhone}</span>
                )}
              </div>
            </div>
          )}

          <div className="post-ad-form__field">
            <label htmlFor="ad-contact-name">Contact name (optional)</label>
            <input
              id="ad-contact-name"
              type="text"
              placeholder="Name buyers should ask for"
              value={contactName}
              maxLength={120}
              onChange={(e) => onChange("contactName", e.target.value)}
            />
            <div className="post-ad-form__hint-row">
              {errors.contactName ? (
                <span className="post-ad-form__error">{errors.contactName}</span>
              ) : (
                contactName.trim() === "" &&
                accountDisplayName && <span className="post-ad-form__hint">Using account name: {accountDisplayName}</span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
