import { FaBullhorn, FaChalkboardTeacher, FaHome, FaLaptop, FaStar, FaUniversity } from "react-icons/fa";
import "./Badge.css";

export function OnlineBadge() {
  return (
    <span className="badge badge--online">
      <FaLaptop aria-hidden="true" /> Online
    </span>
  );
}

export function FeaturedBadge() {
  return (
    <span className="badge badge--featured">
      <FaStar aria-hidden="true" /> Featured
    </span>
  );
}

/** Marks an individual ad's own `promoted` status on an ordinary listing card (ClassCard) - a
 *  boosted/paid placement mixed inline into the organic results grid, as opposed to a dedicated
 *  promo slot (see PromotedBadge) or the shared FEATURED promotion label (see PromotionLabelBadge). */
export function BoostedBadge() {
  return (
    <span className="badge badge--featured">
      <FaStar aria-hidden="true" /> Promoted
    </span>
  );
}

/** Marks a card as sitting in a promotional SLOT (e.g. the Featured Classes carousel) regardless
 *  of whether that slot currently has a paying advertiser - see FeaturedTuitionCard and
 *  FeaturedPlaceholderCard. Distinct from FeaturedBadge, which flags an individual ad's own
 *  `promoted` status on ordinary listing cards (ClassCard) outside any dedicated promo slot. */
export function PromotedBadge() {
  return <span className="badge badge--promoted">Promoted</span>;
}

export function HomeVisitBadge() {
  return (
    <span className="badge badge--home-visit">
      <FaHome aria-hidden="true" /> Home Visit
    </span>
  );
}

export function SponsoredBadge() {
  return (
    <span className="badge badge--sponsored">
      <FaBullhorn aria-hidden="true" /> Sponsored
    </span>
  );
}

/** Renders the FeaturedBadge, SponsoredBadge, or PromotedBadge for a tuition promotion (see tuition/promotion). */
export function PromotionLabelBadge({ label }: { label: "SPONSORED" | "FEATURED" | "PROMOTED" }) {
  if (label === "FEATURED") return <FeaturedBadge />;
  if (label === "PROMOTED") return <PromotedBadge />;
  return <SponsoredBadge />;
}

export function TeacherProfileBadge({ profileType }: { profileType: "TEACHER" | "INSTITUTE" }) {
  return profileType === "INSTITUTE" ? (
    <span className="badge badge--profile">
      <FaUniversity aria-hidden="true" /> Institute
    </span>
  ) : (
    <span className="badge badge--profile">
      <FaChalkboardTeacher aria-hidden="true" /> Teacher
    </span>
  );
}
