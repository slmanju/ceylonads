import { FaBook } from "react-icons/fa";
import { Link } from "react-router-dom";
import { PromotedBadge } from "../Badge/Badge";
import { PromotionLink } from "./PromotionLink";
import type { PromotionTarget } from "../../tuition/promotion/model/promotion";
import "./SpotlightPosterTile.css";

interface SpotlightPosterTileProps {
  imageUrl?: string;
  /** Real tile only - resolves via PromotionLink (AD -> /classes/:slug, EXTERNAL -> url). Ignored
   *  for a placeholder, which always links to /post-ad (the existing Promote a Class flow). */
  target?: PromotionTarget;
  isPlaceholder?: boolean;
  showPromotedBadge?: boolean;
}

// Poster-only promotion tile shared by Homepage Spotlight and Search Page Spotlight's column
// carousels (see HomeSpotlightRail/SearchSpotlightRail) - deliberately image-only, no title,
// price, location, or CTA text: the poster itself is the entire tile, and the whole tile is the
// clickable target. Real and placeholder tiles share the exact same frame (see
// SpotlightPosterTile.css) so every tile in a rail - real or "Advertise Here" - has identical
// width, height, and border radius regardless of the underlying image's own dimensions.
export function SpotlightPosterTile({ imageUrl, target, isPlaceholder = false, showPromotedBadge = true }: SpotlightPosterTileProps) {
  const className = `spotlight-poster-tile${isPlaceholder ? " spotlight-poster-tile--placeholder" : ""}`;

  const content = (
    <>
      {imageUrl ? (
        <img src={imageUrl} alt="" className="spotlight-poster-tile__image" loading="lazy" />
      ) : (
        <div className="spotlight-poster-tile__image spotlight-poster-tile__image--fallback">
          <FaBook aria-hidden="true" />
        </div>
      )}
      {showPromotedBadge && (
        <span className="spotlight-poster-tile__badge">
          <PromotedBadge />
        </span>
      )}
    </>
  );

  if (isPlaceholder) {
    return (
      <Link to="/post-ad" className={className}>
        {content}
      </Link>
    );
  }

  return (
    <PromotionLink target={target!} className={className}>
      {content}
    </PromotionLink>
  );
}
