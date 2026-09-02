import type { Curriculum, DeliveryMode, TeacherProfileType, TuitionLevel } from "../../model/tuition";
import type { PromotionEligibility, TuitionPromotion } from "../model/promotion";

// Central place for "is this promotion allowed to show here" - see tuition-promotion spec section
// "CONTEXT / ELIGIBILITY": matching rules must live in the promotion domain/provider layer, never
// scattered across page components.

export interface PromotionMatchContext {
  subjects?: string[];
  levels?: TuitionLevel[];
  curriculums?: Curriculum[];
  deliveryModes?: DeliveryMode[];
  locationSlugs?: string[];
  profileType?: TeacherProfileType;
}

function overlaps<T>(needed: T[] | undefined, available: T[] | undefined): boolean {
  if (!needed || needed.length === 0) return true;
  if (!available || available.length === 0) return false;
  return needed.some((v) => available.includes(v));
}

function subjectsOverlap(needed: string[] | undefined, available: string[] | undefined): boolean {
  if (!needed || needed.length === 0) return true;
  if (!available || available.length === 0) return false;
  const haystack = available.join(" ").toLowerCase();
  return needed.some((subject) => haystack.includes(subject.toLowerCase()));
}

/** true if a promotion with no eligibility at all is broadly eligible (site-wide banners etc). */
export function isEligible(eligibility: PromotionEligibility | undefined, context: PromotionMatchContext): boolean {
  if (!eligibility) return true;

  if (eligibility.profileType && eligibility.profileType !== context.profileType) return false;
  if (!overlaps(eligibility.levels, context.levels)) return false;
  if (!overlaps(eligibility.curriculums, context.curriculums)) return false;
  if (!overlaps(eligibility.deliveryModes, context.deliveryModes)) return false;
  if (!overlaps(eligibility.locationSlugs, context.locationSlugs)) return false;
  if (!subjectsOverlap(eligibility.subjects, context.subjects)) return false;

  return true;
}

export function isActive(promotion: TuitionPromotion, now: Date = new Date()): boolean {
  if (promotion.activeFrom && now < new Date(promotion.activeFrom)) return false;
  if (promotion.activeTo && now > new Date(`${promotion.activeTo}T23:59:59`)) return false;
  return true;
}

export function byDisplayOrder(a: TuitionPromotion, b: TuitionPromotion): number {
  return a.displayOrder - b.displayOrder;
}
