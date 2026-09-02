import type { PromotionTarget } from "../model/promotion";

// Single place that turns a typed PromotionTarget into a clickable href, so components never
// branch on target.type themselves. AD/TEACHER_PROFILE/INSTITUTE_PROFILE targets resolve via a
// real ad slug once a backend promotion supplies one; until then (and for EXTERNAL targets) the
// mock dataset's `url` is used directly.
export function resolvePromotionTargetHref(target: PromotionTarget): string {
  if (target.adSlug) return `/classes/${target.adSlug}`;
  return target.url ?? "#";
}
