const SRI_LANKA_COUNTRY_CODE = "94";

// Local "0XXXXXXXXX" or international "+94XXXXXXXXX" Sri Lankan mobile/landline numbers.
// Mirrors the backend's Phones.SRI_LANKAN_PHONE_PATTERN.
const SRI_LANKAN_PHONE_PATTERN = /^(0[1-9][0-9]{8}|\+94[1-9][0-9]{8})$/;

function digitsOnly(value: string): string {
  return value.replace(/\D/g, "");
}

/** A blank value is valid (the field is optional); otherwise it must be a recognized SL number. */
export function isValidPhone(value: string): boolean {
  const trimmed = value.trim();
  return trimmed === "" || SRI_LANKAN_PHONE_PATTERN.test(trimmed);
}

export function toInternationalDigits(phone: string): string | null {
  const digits = digitsOnly(phone);
  if (!digits) return null;
  if (digits.startsWith(SRI_LANKA_COUNTRY_CODE)) return digits;
  if (digits.startsWith("0")) return SRI_LANKA_COUNTRY_CODE + digits.slice(1);
  return digits;
}

export function toTelHref(phone: string): string {
  return `tel:${digitsOnly(phone) ? `+${toInternationalDigits(phone)}` : phone}`;
}

export function toWhatsAppHref(phone: string, message?: string): string | null {
  const number = toInternationalDigits(phone);
  if (!number) return null;
  const query = message ? `?text=${encodeURIComponent(message)}` : "";
  return `https://wa.me/${number}${query}`;
}
