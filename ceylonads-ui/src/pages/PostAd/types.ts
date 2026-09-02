import type { MediaResponse } from "../../types/api";

export type StepKey = "category" | "details" | "location" | "photos" | "review";

export type DetailsFieldErrors = Partial<
  Record<"title" | "description" | "price" | "contactName" | "phoneNumber" | "whatsappNumber", string>
>;

export const STEPS: { key: StepKey; label: string }[] = [
  { key: "category", label: "Category" },
  { key: "details", label: "Details" },
  { key: "location", label: "Location" },
  { key: "photos", label: "Photos" },
  { key: "review", label: "Review" },
];

export interface AdFormValues {
  categorySlug: string;
  categoryPath: string;
  title: string;
  description: string;
  price: string;
  // 0..N: whether zero/one/many is actually required depends on the category (and, for
  // Tuition, Class Mode) - enforced by the backend, surfaced here only as a submit error.
  locationSlugs: string[];
  attributes: Record<string, string>;
  // Ad-specific contact override, all optional - blank falls back to the account contact.
  contactName: string;
  phoneNumber: string;
  whatsappNumber: string;
  // Form-only UX affordance (not sent to the backend): when true, the WhatsApp field is hidden
  // and phoneNumber is submitted as the WhatsApp number too.
  whatsappSameAsPhone: boolean;
}

export const EMPTY_FORM_VALUES: AdFormValues = {
  categorySlug: "",
  categoryPath: "",
  title: "",
  description: "",
  price: "",
  locationSlugs: [],
  attributes: {},
  contactName: "",
  phoneNumber: "",
  whatsappNumber: "",
  whatsappSameAsPhone: true,
};

export type PendingPhotoStatus = "pending" | "uploading" | "uploaded" | "error";

export interface PendingPhoto {
  localId: string;
  file: File;
  previewUrl: string;
  status: PendingPhotoStatus;
  progress: number;
  error?: string;
}

export const MAX_PHOTOS = 8;

export interface AdWizardProps {
  mode: "create" | "edit";
  adId?: number;
  initialValues?: AdFormValues;
  initialMedia?: MediaResponse[];
}
