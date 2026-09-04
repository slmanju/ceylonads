// Single source of truth for ezClass's support WhatsApp link - every CTA (Contact page today,
// anything else later) must go through this instead of hardcoding the number.
const RAW_NUMBER = import.meta.env.VITE_EZCLASS_WHATSAPP_NUMBER as string | undefined;

export function buildEzClassWhatsAppLink(message?: string): string {
  const digitsOnly = (RAW_NUMBER ?? "").replace(/\D/g, "");
  const query = message ? `?text=${encodeURIComponent(message)}` : "";
  return `https://wa.me/${digitsOnly}${query}`;
}
