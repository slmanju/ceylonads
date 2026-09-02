import type { MediaResponse } from "../../types/api";

export type StepKey = "category" | "details" | "location" | "photos" | "review";

export type DetailsFieldErrors = Partial<
  Record<"title" | "description" | "price" | "contactName" | "phoneNumber" | "whatsappNumber", string>
>;

export const STEPS: { key: StepKey; label: string }[] = [
  { key: "category", label: "Class Type" },
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
  // 0..N: whether zero/one/many is required depends on the category (and, for tuition, Class
  // Mode) - enforced by the backend, surfaced here only as a submit error.
  locationSlugs: string[];
  attributes: Record<string, string>;
  contactName: string;
  phoneNumber: string;
  whatsappNumber: string;
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
