import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { createTuitionClass, updateTuitionClass } from "../../tuition/api/tuitionApi";
import { getMyProfile } from "../../api/customerApi";
import { uploadAdMedia } from "../../api/mediaApi";
import { useTuitionCategories } from "../../hooks/useTuitionCategories";
import { useLocations } from "../../hooks/useLocations";
import { useCategoryAttributes } from "../../hooks/useCategoryAttributes";
import { validateAttributes } from "../../components/AttributeFields/attributeValidation";
import { getApiErrorMessage } from "../../utils/apiError";
import { isValidPhone } from "../../utils/phone";
import { PostAdStepper } from "./PostAdStepper";
import { CategoryStep } from "./steps/CategoryStep";
import { DetailsStep } from "./steps/DetailsStep";
import { LocationStep } from "./steps/LocationStep";
import { PhotosStep } from "./steps/PhotosStep";
import { ReviewStep } from "./steps/ReviewStep";
import {
  EMPTY_FORM_VALUES,
  STEPS,
  type AdFormValues,
  type AdWizardProps,
  type DetailsFieldErrors,
  type PendingPhoto,
} from "./types";
import type { TuitionClassCreateRequest } from "../../types/api";
import "./PostAdWizard.css";
import "./PostAdStep.css";

type FieldErrors = DetailsFieldErrors;

// Blank fee means "contact for fee" (normalized to 0 at submit time below).
export function normalizePrice(price: string): number {
  const trimmed = price.trim();
  return trimmed === "" ? 0 : Number(trimmed);
}

export function canContinueFromStep(
  step: (typeof STEPS)[number],
  values: AdFormValues,
  attributeDefinitions: Parameters<typeof validateAttributes>[0],
): boolean {
  switch (step.key) {
    case "category":
      return values.categorySlug !== "";
    case "details":
      return (
        Object.keys(validateDetails(values)).length === 0 &&
        Object.keys(validateAttributes(attributeDefinitions, values.attributes)).length === 0
      );
    case "location":
      // Whether zero/one/many locations is required is category- and Class-Mode-dependent; the
      // backend is authoritative, so this step never blocks Continue on location count alone.
      return true;
    default:
      return true;
  }
}

export function validateDetails(values: AdFormValues): FieldErrors {
  const errors: FieldErrors = {};
  if (!values.title.trim()) errors.title = "Please enter a title.";
  else if (values.title.length > 180) errors.title = "Title must be 180 characters or fewer.";

  if (values.description.trim().length > 5000) errors.description = "Description must be 5000 characters or fewer.";

  const priceTrimmed = values.price.trim();
  if (priceTrimmed !== "") {
    const priceNumber = Number(priceTrimmed);
    if (Number.isNaN(priceNumber)) errors.price = "Please enter a valid fee.";
    else if (priceNumber < 0) errors.price = "Fee can't be negative.";
  }

  if (values.contactName.length > 120) errors.contactName = "Contact name must be 120 characters or fewer.";
  if (!isValidPhone(values.phoneNumber)) {
    errors.phoneNumber = "Enter a valid Sri Lankan phone number, e.g. 0712345678 or +94712345678.";
  }
  if (!values.whatsappSameAsPhone && !isValidPhone(values.whatsappNumber)) {
    errors.whatsappNumber = "Enter a valid Sri Lankan phone number, e.g. 0712345678 or +94712345678.";
  }

  return errors;
}

export function PostAdWizard({ mode, adId, initialValues, initialMedia }: AdWizardProps) {
  const navigate = useNavigate();
  const { subcategories, loading: categoriesLoading, error: categoriesError } = useTuitionCategories();
  const { locations, loading: locationsLoading, error: locationsError } = useLocations();

  const initialStepIndex = mode === "edit" ? STEPS.length - 1 : 0;
  const [stepIndex, setStepIndex] = useState(initialStepIndex);
  const [maxReachedIndex, setMaxReachedIndex] = useState(initialStepIndex);
  const [values, setValues] = useState<AdFormValues>(initialValues ?? EMPTY_FORM_VALUES);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [attributeErrors, setAttributeErrors] = useState<Record<string, string>>({});
  const { definitions: attributeDefinitions } = useCategoryAttributes(values.categorySlug);
  const [existingMedia] = useState(initialMedia ?? []);
  const [pendingPhotos, setPendingPhotos] = useState<PendingPhoto[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [completedAdId, setCompletedAdId] = useState<number | null>(null);
  const [accountContact, setAccountContact] = useState<{ displayName: string; phone: string | null } | null>(null);

  const photosRef = useRef<PendingPhoto[]>([]);

  useEffect(() => {
    let cancelled = false;
    getMyProfile()
      .then((profile) => {
        if (!cancelled) setAccountContact({ displayName: profile.displayName, phone: profile.phone });
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    photosRef.current = pendingPhotos;
  }, [pendingPhotos]);

  useEffect(() => {
    return () => {
      photosRef.current.forEach((p) => URL.revokeObjectURL(p.previewUrl));
    };
  }, []);

  const goToStep = (index: number) => {
    setStepIndex(index);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleContinue = () => {
    if (STEPS[stepIndex].key === "details") {
      const errors = validateDetails(values);
      setFieldErrors(errors);
      const attrErrors = validateAttributes(attributeDefinitions, values.attributes);
      setAttributeErrors(attrErrors);
      if (Object.keys(errors).length > 0 || Object.keys(attrErrors).length > 0) return;
    }
    const next = Math.min(stepIndex + 1, STEPS.length - 1);
    setMaxReachedIndex((m) => Math.max(m, next));
    goToStep(next);
  };

  const handleBack = () => goToStep(Math.max(0, stepIndex - 1));

  const addFiles = (files: File[]) => {
    const newPhotos: PendingPhoto[] = files.map((file) => ({
      localId: crypto.randomUUID(),
      file,
      previewUrl: URL.createObjectURL(file),
      status: "pending",
      progress: 0,
    }));
    setPendingPhotos((prev) => [...prev, ...newPhotos]);
  };

  const removePhoto = (localId: string) => {
    setPendingPhotos((prev) => {
      const target = prev.find((p) => p.localId === localId);
      if (target) URL.revokeObjectURL(target.previewUrl);
      return prev.filter((p) => p.localId !== localId);
    });
  };

  const uploadPhoto = async (targetAdId: number, photo: PendingPhoto): Promise<boolean> => {
    setPendingPhotos((prev) => prev.map((p) => (p.localId === photo.localId ? { ...p, status: "uploading", progress: 0, error: undefined } : p)));
    try {
      await uploadAdMedia(targetAdId, photo.file, (progress) => {
        setPendingPhotos((prev) => prev.map((p) => (p.localId === photo.localId ? { ...p, progress } : p)));
      });
      setPendingPhotos((prev) => prev.map((p) => (p.localId === photo.localId ? { ...p, status: "uploaded", progress: 100 } : p)));
      return true;
    } catch (err) {
      setPendingPhotos((prev) =>
        prev.map((p) => (p.localId === photo.localId ? { ...p, status: "error", error: getApiErrorMessage(err, "Upload failed") } : p)),
      );
      return false;
    }
  };

  const retryPhoto = (localId: string) => {
    if (completedAdId === null) return;
    const photo = pendingPhotos.find((p) => p.localId === localId);
    if (photo) void uploadPhoto(completedAdId, photo);
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setSubmitError(null);

    let targetAdId = completedAdId;

    if (targetAdId === null) {
      const payload: TuitionClassCreateRequest = {
        title: values.title.trim(),
        description: values.description.trim(),
        price: normalizePrice(values.price),
        categorySlug: values.categorySlug,
        locationSlugs: values.locationSlugs,
        contactName: values.contactName.trim(),
        phoneNumber: values.phoneNumber.trim(),
        whatsappNumber: (values.whatsappSameAsPhone ? values.phoneNumber : values.whatsappNumber).trim(),
        subject: values.attributes.subject || undefined,
        level: values.attributes.grade || undefined,
        curriculum: values.attributes.curriculum || undefined,
        medium: values.attributes.medium ? values.attributes.medium.split(",").filter(Boolean) : undefined,
        deliveryMode: values.attributes.classMode || undefined,
        classFormat: values.attributes.classType || undefined,
      };

      try {
        if (mode === "create") {
          const created = await createTuitionClass(payload);
          targetAdId = created.id;
        } else {
          await updateTuitionClass(adId!, payload);
          targetAdId = adId!;
        }
        setCompletedAdId(targetAdId);
      } catch (err) {
        setSubmitError(
          getApiErrorMessage(err, mode === "create" ? "Could not create your class. Please try again." : "Could not save your changes. Please try again."),
        );
        setSubmitting(false);
        return;
      }
    }

    const toUpload = pendingPhotos.filter((p) => p.status !== "uploaded");
    let anyFailed = false;
    for (const photo of toUpload) {
      const ok = await uploadPhoto(targetAdId, photo);
      if (!ok) anyFailed = true;
    }

    setSubmitting(false);
    if (anyFailed) return;

    navigate("/my-ads", { state: { flash: mode === "create" ? "created" : "updated" } });
  };

  const step = STEPS[stepIndex];
  const isFirstStep = stepIndex === 0;
  const isReviewStep = step.key === "review";
  const online = values.attributes.classMode === "ONLINE";

  return (
    <div className="post-ad-wizard container">
      <h1 className="post-ad-wizard__title">{mode === "edit" ? "Edit your class" : "Post a class"}</h1>

      <PostAdStepper currentIndex={stepIndex} maxReachedIndex={maxReachedIndex} onStepClick={goToStep} />

      <div className="post-ad-wizard__panel">
        {step.key === "category" && (
          <CategoryStep
            subcategories={subcategories}
            loading={categoriesLoading}
            error={categoriesError}
            categorySlug={values.categorySlug}
            onSelect={(categorySlug, categoryName) =>
              setValues((v) => ({
                ...v,
                categorySlug,
                categoryPath: categoryName,
                attributes: categorySlug === v.categorySlug ? v.attributes : {},
              }))
            }
          />
        )}

        {step.key === "details" && (
          <DetailsStep
            title={values.title}
            description={values.description}
            price={values.price}
            categoryPath={values.categoryPath}
            errors={fieldErrors}
            onChange={(field, value) => setValues((v) => ({ ...v, [field]: value }))}
            contactName={values.contactName}
            phoneNumber={values.phoneNumber}
            whatsappNumber={values.whatsappNumber}
            whatsappSameAsPhone={values.whatsappSameAsPhone}
            onWhatsappSameAsPhoneChange={(checked) => setValues((v) => ({ ...v, whatsappSameAsPhone: checked }))}
            accountDisplayName={accountContact?.displayName}
            accountPhone={accountContact?.phone}
            attributeDefinitions={attributeDefinitions}
            attributeValues={values.attributes}
            attributeErrors={attributeErrors}
            onAttributeChange={(key, value) => setValues((v) => ({ ...v, attributes: { ...v.attributes, [key]: value } }))}
          />
        )}

        {step.key === "location" && (
          <LocationStep
            locations={locations}
            loading={locationsLoading}
            error={locationsError}
            locationSlugs={values.locationSlugs}
            onChange={(locationSlugs) => setValues((v) => ({ ...v, locationSlugs }))}
            online={online}
          />
        )}

        {step.key === "photos" && (
          <PhotosStep
            existingMedia={existingMedia}
            pendingPhotos={pendingPhotos}
            onAddFiles={addFiles}
            onRemovePhoto={removePhoto}
            onRetryPhoto={completedAdId !== null ? retryPhoto : undefined}
          />
        )}

        {isReviewStep && (
          <ReviewStep
            values={values}
            attributeDefinitions={attributeDefinitions}
            locations={locations}
            existingMedia={existingMedia}
            pendingPhotos={pendingPhotos}
            mode={mode}
            submitting={submitting}
            submitError={submitError}
            onEditStep={goToStep}
            onSubmit={handleSubmit}
          />
        )}

        {completedAdId !== null && pendingPhotos.some((p) => p.status === "error") && (
          <button type="button" className="btn btn-secondary btn-block" onClick={() => navigate("/my-ads")}>
            Continue to My Classes anyway
          </button>
        )}
      </div>

      {!isReviewStep && (
        <div className="post-ad-wizard__actions">
          <button type="button" className="btn btn-secondary" onClick={handleBack} disabled={isFirstStep}>
            Back
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleContinue}
            disabled={!canContinueFromStep(step, values, attributeDefinitions)}
          >
            Continue
          </button>
        </div>
      )}

      {isReviewStep && stepIndex > 0 && (
        <div className="post-ad-wizard__actions">
          <button type="button" className="btn btn-secondary" onClick={handleBack} disabled={submitting}>
            Back
          </button>
        </div>
      )}
    </div>
  );
}
