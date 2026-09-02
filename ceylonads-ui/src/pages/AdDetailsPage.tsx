import { useEffect, useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { isAxiosError } from "axios";
import { FaMapMarkerAlt, FaRegClock, FaUserCircle, FaTag, FaPhone, FaWhatsapp } from "react-icons/fa";
import { getAd } from "../api/adsApi";
import { ImageGallery } from "../components/ImageGallery/ImageGallery";
import { ClampedText } from "../components/ClampedText/ClampedText";
import { AdDetailSidebarPromotion } from "../components/AdDetailSidebarPromotion/AdDetailSidebarPromotion";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import { Breadcrumbs } from "../components/Breadcrumbs/Breadcrumbs";
import { Seo } from "../components/Seo/Seo";
import { useCategories } from "../hooks/useCategories";
import type { AdResponse } from "../types/api";
import { formatAdPrice } from "../utils/formatPrice";
import { formatAdLocations } from "../utils/formatLocations";
import { formatRelativeDate } from "../utils/formatDate";
import { getApiErrorMessage } from "../utils/apiError";
import { toTelHref, toWhatsAppHref } from "../utils/phone";
import { resolveMediaUrl } from "../api/apiClient";
import { categoryAncestors } from "../utils/categoryHierarchy";
import { absoluteUrl, buildBreadcrumbJsonLd, truncateDescription, DEFAULT_OG_IMAGE } from "../utils/seo";
import "./AdDetailsPage.css";

export function AdDetailsPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { categories } = useCategories();
  const [ad, setAd] = useState<AdResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!slug) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    setNotFound(false);

    getAd(slug)
      .then((data) => {
        if (!cancelled) setAd(data);
      })
      .catch((err) => {
        if (cancelled) return;
        // A genuine "no such ad" gets its own dedicated state (below); anything else (500,
        // network failure, timeout) falls back to the general error state instead of
        // misleadingly telling the user the ad doesn't exist.
        if (isAxiosError(err) && err.response?.status === 404) {
          setNotFound(true);
        } else {
          setError(getApiErrorMessage(err, "This ad could not be loaded."));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [slug]);

  // The id in the URL is authoritative; once the ad loads, settle on its canonical slug so
  // shares/bookmarks/crawls converge on one URL instead of splitting signal across a bare id,
  // a stale title-slug, and the current title-slug.
  useEffect(() => {
    if (ad && slug !== ad.slug) {
      navigate(`/ads/${ad.slug}`, { replace: true });
    }
  }, [ad, slug, navigate]);

  if (loading) {
    return (
      <div className="container ad-details-page">
        <LoadingState label="Loading ad…" />
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="container ad-details-page">
        <Seo title="Ad not found" noindex />
        <EmptyState
          title="Ad not found"
          message="This ad may have been removed, expired, or the link may be incorrect."
          action={
            <Link to="/ads" className="btn btn-primary">
              Browse Ads
            </Link>
          }
        />
      </div>
    );
  }

  if (error || !ad) {
    return (
      <div className="container ad-details-page">
        <Seo title="Ad not found" noindex />
        <ErrorState title="Something went wrong" message={error ?? "This ad is unavailable."} />
      </div>
    );
  }

  const category = categories.find((c) => c.slug === ad.categorySlug);
  const ancestors = category ? categoryAncestors(categories, category) : [];
  // Tuition promotional artwork is portrait (teacher poster with subject/schedule/contact
  // details near every edge), unlike the landscape photos typical of every other category -
  // this only swaps the gallery/layout treatment, never the shared gallery component itself.
  const isTuition = ancestors[0]?.slug === "education-tuition";
  const locationLabel = formatAdLocations(ad.locations);
  const breadcrumbItems = [
    { name: "Home", path: "/" },
    { name: "Browse", path: "/ads" },
    ...ancestors.map((c) => ({ name: c.name, path: `/ads?category=${c.slug}` })),
    { name: ad.title },
  ];

  const canonicalPath = `/ads/${ad.slug}`;
  const canonicalUrl = absoluteUrl(canonicalPath);
  const primaryImage = ad.media[0] ? resolveMediaUrl(ad.media[0].url) : absoluteUrl(DEFAULT_OG_IMAGE);

  // Resolved ad-specific contact (falls back to the seller's account contact on the backend) -
  // never the raw seller/account fields, so an ad-specific override actually takes effect here.
  const contactName = ad.contact?.name ?? ad.seller.displayName;
  const contactPhone = ad.contact?.phoneNumber ?? null;
  const contactWhatsapp = ad.contact?.whatsappNumber ?? null;

  const whatsappHref = contactWhatsapp
    ? toWhatsAppHref(contactWhatsapp, `Hi, I'm interested in your ad "${ad.title}" on CeylonAds.`)
    : undefined;

  const sellerRow = (
    <div className="ad-details-page__seller">
      <FaUserCircle aria-hidden="true" className="ad-details-page__seller-icon" />
      <div>
        <p className="ad-details-page__seller-label">Contact</p>
        <p className="ad-details-page__seller-name">{contactName}</p>
      </div>
    </div>
  );

  const contactButtons = contactPhone || whatsappHref ? (
    <>
      {contactPhone && (
        <a className="btn btn-primary" href={toTelHref(contactPhone)}>
          <FaPhone aria-hidden="true" /> Call
        </a>
      )}
      {whatsappHref && (
        <a
          className="btn btn-outline ad-details-page__whatsapp"
          href={whatsappHref}
          target="_blank"
          rel="noopener noreferrer"
        >
          <FaWhatsapp aria-hidden="true" /> WhatsApp
        </a>
      )}
    </>
  ) : null;

  const jsonLd = [
    buildBreadcrumbJsonLd(breadcrumbItems),
    {
      "@context": "https://schema.org",
      "@type": "Product",
      name: ad.title,
      description: ad.description,
      image: ad.media.map((m) => resolveMediaUrl(m.url)),
      url: canonicalUrl,
      // A price of 0 means "contact for price", not a genuinely free product - omitting the
      // Offer entirely avoids misrepresenting it to search engines as free.
      ...(ad.price > 0
        ? {
            offers: {
              "@type": "Offer",
              price: ad.price,
              priceCurrency: "LKR",
              availability: "https://schema.org/InStock",
              url: canonicalUrl,
              seller: {
                "@type": "Person",
                name: contactName,
              },
            },
          }
        : {}),
    },
  ];

  return (
    <div className="container ad-details-page">
      <Seo
        title={locationLabel ? `${ad.title} for Sale in ${locationLabel}` : ad.title}
        description={truncateDescription(ad.description)}
        canonicalPath={canonicalPath}
        ogType="product"
        ogImage={primaryImage}
        jsonLd={jsonLd}
      />
      <Breadcrumbs items={breadcrumbItems} />

      <div className="ad-details-page__header">
        <h1 className="ad-details-page__title">{ad.title}</h1>
        <div className="ad-details-page__meta">
          {locationLabel && (
            <span>
              <FaMapMarkerAlt aria-hidden="true" /> {locationLabel}
            </span>
          )}
          <span>
            <FaRegClock aria-hidden="true" /> {formatRelativeDate(ad.publishedAt ?? ad.createdAt)}
          </span>
          <span>
            <FaTag aria-hidden="true" /> {ad.category}
          </span>
        </div>
      </div>

      <p className="ad-details-page__price ad-details-page__price--mobile">{formatAdPrice(ad.price)}</p>

      <div className={`ad-details-page__layout ${isTuition ? "ad-details-page__layout--portrait" : ""}`.trim()}>
        <div className="ad-details-page__gallery">
          <ImageGallery media={ad.media} title={ad.title} variant={isTuition ? "portrait" : "landscape"} />
        </div>

        <div className="ad-details-page__side">
          <aside className="ad-details-page__contact-card">
            <p className="ad-details-page__price">{formatAdPrice(ad.price)}</p>
            {sellerRow}
            {contactButtons && <div className="ad-details-page__contact">{contactButtons}</div>}
          </aside>
          <AdDetailSidebarPromotion
            categoryName={ad.category}
            className="ad-details-page__sidebar-promo--desktop"
          />
        </div>
      </div>

      {contactButtons && (
        <div className="ad-details-page__contact ad-details-page__contact--mobile">{contactButtons}</div>
      )}

      <section className="ad-details-page__seller-block--mobile">
        <h2>Contact</h2>
        {sellerRow}
      </section>

      <div className="ad-details-page__content">
        {ad.attributes.length > 0 && (
          <section className="ad-details-page__details">
            <h2>Details</h2>
            <dl>
              {ad.attributes.map((attribute) => (
                <div key={attribute.key}>
                  <dt>{attribute.name}</dt>
                  <dd>
                    {attribute.displayValue}
                    {attribute.unit ? ` ${attribute.unit}` : ""}
                  </dd>
                </div>
              ))}
            </dl>
          </section>
        )}

        <section className="ad-details-page__description">
          <h2>Description</h2>
          <ClampedText text={ad.description} maxLines={6} />
        </section>

        <AdDetailSidebarPromotion categoryName={ad.category} className="ad-details-page__sidebar-promo--mobile" />
      </div>
    </div>
  );
}
