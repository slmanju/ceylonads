import { Link } from "react-router-dom";
import { FaBook, FaChalkboardTeacher, FaGraduationCap, FaMapMarkerAlt } from "react-icons/fa";
import type { AdResponse } from "../../types/api";
import type { TuitionDetails } from "../../tuition/model/tuition";
import { resolveMediaUrl } from "../../api/apiClient";
import { formatAdPrice } from "../../utils/formatPrice";
import { formatAdLocations } from "../../utils/formatLocations";
import { formatRelativeDate } from "../../utils/formatDate";
import { formatSessionCardLine, primarySession } from "../../utils/formatSchedule";
import { getAttrDisplay, isOnlineClass } from "../../utils/tuitionAttributes";
import { BoostedBadge, HomeVisitBadge, OnlineBadge } from "../Badge/Badge";
import "./ClassCard.css";

interface ClassCardProps {
  ad: AdResponse;
  /** Tuition-specific metadata from the mock provider - omitted while it's still loading. */
  details?: TuitionDetails;
}

export function ClassCard({ ad, details }: ClassCardProps) {
  const image = ad.media[0];
  const subject = getAttrDisplay(ad, "subject");
  const grade = getAttrDisplay(ad, "grade");
  const medium = getAttrDisplay(ad, "medium");
  const locationLabel = formatAdLocations(ad.locations);
  const online = isOnlineClass(ad);
  const nextSession = details ? primarySession(details.sessions) : undefined;
  const homeVisit = details?.homeVisit?.available ?? false;

  return (
    <div className="class-card">
      <Link to={`/classes/${ad.slug}`} className="class-card__link">
        <div className="class-card__image-wrap">
          {image ? (
            <img className="class-card__image" src={resolveMediaUrl(image.url)} alt={ad.title} loading="lazy" />
          ) : (
            <div className="class-card__image class-card__image--fallback">
              <FaBook aria-hidden="true" />
            </div>
          )}
          <div className="class-card__badges">
            {ad.promoted && <BoostedBadge />}
            {online && <OnlineBadge />}
            {homeVisit && <HomeVisitBadge />}
          </div>
        </div>

        <div className="class-card__body">
          {subject && <p className="class-card__subject">{subject}</p>}
          <p className="class-card__title" title={ad.title}>
            {ad.title}
          </p>

          <div className="class-card__tags">
            {grade && <span className="class-card__tag">{grade}</span>}
            {medium && <span className="class-card__tag">{medium}</span>}
          </div>

          <div className="class-card__meta">
            {nextSession ? (
              <span className="class-card__meta-item" title={formatSessionCardLine(nextSession)}>
                <FaMapMarkerAlt aria-hidden="true" />
                <span className="class-card__meta-text">{formatSessionCardLine(nextSession)}</span>
              </span>
            ) : (
              locationLabel && (
                <span className="class-card__meta-item" title={locationLabel}>
                  <FaMapMarkerAlt aria-hidden="true" />
                  <span className="class-card__meta-text">{locationLabel}</span>
                </span>
              )
            )}
            <span className="class-card__meta-item" title={ad.seller.displayName}>
              <FaChalkboardTeacher aria-hidden="true" />
              <span className="class-card__meta-text">{ad.seller.displayName}</span>
            </span>
          </div>

          <div className="class-card__footer">
            <p className="class-card__price">
              <FaGraduationCap aria-hidden="true" />
              {formatAdPrice(ad.price)}
            </p>
            <span className="class-card__date">{formatRelativeDate(ad.publishedAt ?? ad.createdAt)}</span>
          </div>
        </div>
      </Link>
    </div>
  );
}
