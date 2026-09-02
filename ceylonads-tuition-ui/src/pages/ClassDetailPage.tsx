import { useEffect, useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { isAxiosError } from "axios";
import { FaChalkboardTeacher, FaMapMarkerAlt, FaPhone, FaRegClock, FaWhatsapp } from "react-icons/fa";
import { ImageGallery } from "../components/ImageGallery/ImageGallery";
import { ClampedText } from "../components/ClampedText/ClampedText";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import { Seo } from "../components/Seo/Seo";
import { OnlineBadge } from "../components/Badge/Badge";
import { useDetailPromotions } from "../hooks/useTuitionPromotions";
import { useFeaturedTuition } from "../hooks/useFeaturedTuition";
import { featuredCardToPromotion } from "../tuition/promotion/api/tuitionPromotionApi";
import { PromotionSideCard } from "../components/Promotion/PromotionSideCard";
import { PromotionSelfAd } from "../components/Promotion/PromotionSelfAd";
import { PromotionBanner } from "../components/Promotion/PromotionBanner";
import { FeaturedTuitionCarousel } from "../components/FeaturedTuitionCarousel/FeaturedTuitionCarousel";
import { SearchPromoCard } from "../components/SearchPromoCard/SearchPromoCard";
import { SearchPromoPlaceholderCard } from "../components/SearchPromoCard/SearchPromoPlaceholderCard";
import { tuitionRepository } from "../tuition/api/tuitionApi";
import type { DeliveryMode } from "../tuition/model/tuition";
import type { TuitionClassDetailResponse } from "../types/api";
import { formatAdPrice } from "../utils/formatPrice";
import { formatAdLocations } from "../utils/formatLocations";
import { formatRelativeDate } from "../utils/formatDate";
import { getApiErrorMessage } from "../utils/apiError";
import { toTelHref, toWhatsAppHref } from "../utils/phone";
import { curriculumEnumFromValue } from "../utils/tuitionAttributes";
import { truncateDescription } from "../utils/seo";
import "./ClassDetailPage.css";

// classMode is a single SELECT attribute (PHYSICAL/ONLINE/BOTH), not the mock layer's richer
// per-session DeliveryMode set - "BOTH" is represented as offering both modes, not as HYBRID
// (a different, per-session concept the real backend doesn't track).
function deliveryModesForPromotion(classModeValue: string | undefined): DeliveryMode[] {
  if (classModeValue === "ONLINE") return ["ONLINE"];
  if (classModeValue === "PHYSICAL") return ["PHYSICAL"];
  if (classModeValue === "BOTH") return ["PHYSICAL", "ONLINE"];
  return [];
}

// Fixed page-level promotional inventory for the Tuition detail page's top carousel - its own
// slot (TUITION_DETAIL_TOP_CAROUSEL), independently configurable/purchasable from the search
// page's TUITION_FEATURED carousel (see ClassesPage.tsx), even though both render through the
// same compact FeaturedTuitionCarousel + SearchPromoCard/SearchPromoPlaceholderCard components.
// Same 12-slot capacity/backfill convention as the search page, and excludes the ad currently
// being viewed so a listing is never shown promoted immediately above itself.
const DETAIL_TOP_CAROUSEL_SLOT = "TUITION_DETAIL_TOP_CAROUSEL";
const DETAIL_TOP_CAROUSEL_SLOT_COUNT = 12;

// Detail Page Spotlight, the right-side/sidebar placement - its own real, independently-
// purchasable slot (TUITION_DETAIL_RIGHT), never TUITION_DETAIL_TOP_CAROUSEL. Requested as a
// single card (size=1), not a carousel - see the aside/mobile-promo JSX below.
const DETAIL_RIGHT_SLOT = "TUITION_DETAIL_RIGHT";

export function ClassDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<TuitionClassDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  // banner remains mock-sourced (TUITION_DETAIL_BANNER has no real backend slot yet - out of
  // scope here); the side/right-rail promotion below no longer comes from this call.
  const { banner: detailBannerPromotion } = useDetailPromotions(
    detail
      ? {
          categorySlug: detail.categorySlug,
          subjectLabel: detail.academic.subject ?? undefined,
          // No reliable mapping from the real free-text "grade" attribute to the promotion
          // layer's coarse TuitionLevel enum - left unset rather than guessed, so level-scoped
          // promotions simply aren't considered eligible here (never a false match).
          level: undefined,
          curriculum: curriculumEnumFromValue(detail.academic.curriculum?.value),
          deliveryModes: deliveryModesForPromotion(detail.classInfo.deliveryModes[0]?.value),
          locationSlugs: detail.locations.map((l) => l.slug),
          // No teacher/institute profile data source today (see TuitionClassDetailResponse -
          // sessions/homeVisit/teacher stay mock-only elsewhere in the app, not on this real
          // detail page), so profile-targeted promotions never match here.
          profileType: undefined,
        }
      : null,
  );

  const { featured: topPromotions, loading: topPromotionsLoading } = useFeaturedTuition(DETAIL_TOP_CAROUSEL_SLOT_COUNT, {
    slot: DETAIL_TOP_CAROUSEL_SLOT,
    excludeAdId: detail?.id,
  });
  const topPromotionsPlaceholderCount = topPromotionsLoading
    ? 0
    : Math.max(0, DETAIL_TOP_CAROUSEL_SLOT_COUNT - topPromotions.length);

  const { featured: detailRightFeatured } = useFeaturedTuition(1, {
    slot: DETAIL_RIGHT_SLOT,
    excludeAdId: detail?.id,
  });
  const detailSidePromotion = detailRightFeatured[0]
    ? featuredCardToPromotion(detailRightFeatured[0], "TUITION_DETAIL_RIGHT")
    : undefined;

  useEffect(() => {
    if (!slug) return;
    let cancelled = false;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    setNotFound(false);

    tuitionRepository
      .getClassDetail(slug, controller.signal)
      .then((data) => {
        if (!cancelled) setDetail(data);
      })
      .catch((err) => {
        if (cancelled || (isAxiosError(err) && err.code === "ERR_CANCELED")) return;
        if (isAxiosError(err) && err.response?.status === 404) {
          setNotFound(true);
        } else {
          setError(getApiErrorMessage(err, "This class could not be loaded."));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [slug]);

  // The id in the URL is authoritative; once the class loads, settle on its canonical slug.
  useEffect(() => {
    if (detail && slug !== detail.slug) {
      navigate(`/classes/${detail.slug}`, { replace: true });
    }
  }, [detail, slug, navigate]);

  if (loading) {
    return (
      <div className="container class-detail-page">
        <LoadingState label="Loading class…" />
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="container class-detail-page">
        <Seo title="Class not found" noindex />
        <EmptyState
          title="Class not found"
          message="This ad may have been removed, expired, or the link may be incorrect."
          action={
            <Link to="/classes" className="btn btn-primary">
              Browse Classes
            </Link>
          }
        />
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="container class-detail-page">
        <Seo title="Class not found" noindex />
        <ErrorState title="Something went wrong" message={error ?? "This class is unavailable."} />
      </div>
    );
  }

  const locationLabel = formatAdLocations(detail.locations);
  const deliveryValues = detail.classInfo.deliveryModes.map((m) => m.value);
  const online = deliveryValues.some((v) => v === "ONLINE" || v === "BOTH");
  // An online-only listing has no physical location to show; Location only earns a row once a
  // physical/both delivery mode is actually present (see CLAUDE.md "Do not invent information
  // not returned by the backend" - this page doesn't have real Home Visit/service-area data).
  const isOnlineOnly = deliveryValues.length > 0 && deliveryValues.every((v) => v === "ONLINE");

  const subjectValue = detail.academic.subject;
  const levelValue = detail.academic.level;
  const curriculumValue = detail.academic.curriculum?.label ?? null;
  const mediumLabels = detail.academic.medium.length ? detail.academic.medium.map((m) => m.label) : null;

  // Academic identity line directly under the title, e.g. "Local • O/L • Mathematics • English
  // Medium" - so subject/level/curriculum/medium never require scrolling.
  const academicIdentity = [curriculumValue, levelValue, subjectValue, mediumLabels ? `${mediumLabels.join(" & ")} Medium` : null]
    .filter((part): part is string => !!part)
    .join(" • ");

  // One consolidated fact grid (rendered in the sidebar, not below the poster) - only real values
  // are ever pushed in. `wide` rows span both columns of the compact mini-grid; the rest pair up.
  const classDetailsRows: { label: string; value: string; wide?: boolean }[] = [];
  if (subjectValue) classDetailsRows.push({ label: "Subject", value: subjectValue, wide: true });
  if (levelValue) classDetailsRows.push({ label: "Grade / Level", value: levelValue });
  if (curriculumValue) classDetailsRows.push({ label: "Curriculum", value: curriculumValue });
  if (mediumLabels) classDetailsRows.push({ label: "Medium", value: mediumLabels.join(", ") });
  if (detail.classInfo.classFormats.length) {
    classDetailsRows.push({ label: "Class Format", value: detail.classInfo.classFormats.map((f) => f.label).join(", ") });
  }
  if (detail.classInfo.deliveryModes.length) {
    classDetailsRows.push({ label: "Delivery", value: detail.classInfo.deliveryModes.map((m) => m.label).join(", ") });
  }
  if (locationLabel && !isOnlineOnly) classDetailsRows.push({ label: "Location", value: locationLabel });
  if (detail.classInfo.classPurposes.length) {
    classDetailsRows.push({ label: "Purpose", value: detail.classInfo.classPurposes.map((p) => p.label).join(" • "), wide: true });
  }

  const classDetailsCard =
    classDetailsRows.length > 0 ? (
      <section className="class-detail-page__details">
        <h2>Class Details</h2>
        <dl>
          {classDetailsRows.map((row) => (
            <div key={row.label} className={row.wide ? "class-detail-page__details-row--wide" : undefined}>
              <dt>{row.label}</dt>
              <dd>{row.value}</dd>
            </div>
          ))}
        </dl>
      </section>
    ) : null;

  const contactName = detail.contact.name;
  const contactPhone = detail.contact.phoneNumber;
  const contactWhatsapp = detail.contact.whatsappNumber;

  const whatsappHref = contactWhatsapp
    ? toWhatsAppHref(contactWhatsapp, `Hi, I'm interested in your class "${detail.title}" on ezClass.`)
    : undefined;

  const contactButtons =
    contactPhone || whatsappHref ? (
      <>
        {contactPhone && (
          <a className="btn btn-primary" href={toTelHref(contactPhone)}>
            <FaPhone aria-hidden="true" /> Call
          </a>
        )}
        {whatsappHref && (
          <a className="btn btn-outline" href={whatsappHref} target="_blank" rel="noopener noreferrer">
            <FaWhatsapp aria-hidden="true" /> WhatsApp
          </a>
        )}
      </>
    ) : null;

  return (
    <div className="container class-detail-page">
      <Seo
        title={locationLabel ? `${detail.title} — ${locationLabel}` : detail.title}
        description={truncateDescription(detail.description || detail.title)}
      />

      <section className="class-detail-page__top-promotions">
        <FeaturedTuitionCarousel
          items={topPromotions}
          loading={topPromotionsLoading}
          placeholderCount={topPromotionsPlaceholderCount}
          compact
          renderItem={(card) => <SearchPromoCard card={card} />}
          renderPlaceholder={() => <SearchPromoPlaceholderCard />}
        />
      </section>

      <div className="class-detail-page__header">
        <div className="class-detail-page__badges">{online && <OnlineBadge />}</div>
        <h1 className="class-detail-page__title">{detail.title}</h1>
        {academicIdentity && <p className="class-detail-page__identity">{academicIdentity}</p>}
        <div className="class-detail-page__meta">
          {locationLabel && (
            <span>
              <FaMapMarkerAlt aria-hidden="true" /> {locationLabel}
            </span>
          )}
          <span>
            <FaRegClock aria-hidden="true" /> {formatRelativeDate(detail.publishedAt ?? detail.createdAt)}
          </span>
        </div>
      </div>

      <div className="class-detail-page__layout">
        <div className="class-detail-page__gallery">
          <ImageGallery media={detail.media} title={detail.title} />

          {/* Mobile/tablet only (see breakpoint below) - the sidebar carrying the desktop copy of
              this same card is hidden there, so Class Details still appears right after the
              thumbnails instead of disappearing. */}
          <div className="class-detail-page__details-mobile">{classDetailsCard}</div>

          {detailBannerPromotion && <PromotionBanner promotion={detailBannerPromotion} size="compact" />}

          {detail.description && (
            <section className="class-detail-page__description">
              <h2>About This Class</h2>
              <ClampedText text={detail.description} maxLines={6} />
            </section>
          )}
        </div>

        <aside className="class-detail-page__side">
          <div className="class-detail-page__contact-card">
            <p className="class-detail-page__price">{formatAdPrice(detail.price)}</p>

            <div className="class-detail-page__seller">
              <FaChalkboardTeacher aria-hidden="true" className="class-detail-page__seller-icon" />
              <div>
                <p className="class-detail-page__seller-label">Tutor / Contact</p>
                <p className="class-detail-page__seller-name">{contactName}</p>
              </div>
            </div>

            {contactButtons && <div className="class-detail-page__contact-buttons">{contactButtons}</div>}

            {contactButtons && <p className="class-detail-page__contact-note">Contact the tutor directly</p>}
          </div>

          {classDetailsCard}

          {detailSidePromotion ? <PromotionSideCard promotion={detailSidePromotion} /> : <PromotionSelfAd />}
        </aside>
      </div>

      {contactButtons && (
        <div className="class-detail-page__mobile-bar">
          <p className="class-detail-page__mobile-bar-price">{formatAdPrice(detail.price)}</p>
          <div className="class-detail-page__contact-buttons">{contactButtons}</div>
        </div>
      )}

      {/* Mobile-only: the aside above is hidden below the stacking breakpoint, and sponsored
          content must never appear above the seller/contact info it sits below here. */}
      <div className="class-detail-page__promo-mobile">
        {detailSidePromotion ? <PromotionSideCard promotion={detailSidePromotion} /> : <PromotionSelfAd />}
      </div>
    </div>
  );
}
